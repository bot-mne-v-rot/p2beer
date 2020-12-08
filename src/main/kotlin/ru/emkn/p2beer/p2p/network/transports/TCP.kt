package ru.emkn.p2beer.p2p.network.transports

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.traits.*
import ru.emkn.p2beer.p2p.network.transports.messaging.*

import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.nio.ByteBuffer
import java.net.InetSocketAddress
import java.net.StandardSocketOptions.*

import kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlin.random.*

private class ContCompletionHandler<T> : CompletionHandler<T, Continuation<T>> {
    override fun completed(result: T, attachment: Continuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: Continuation<T>) {
        attachment.resumeWithException(exc)
    }
}

private class TCPSocket(val channel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()) : Socket {
    init {
        channel.setOption(SO_REUSEADDR, true)
        channel.setOption(SO_REUSEPORT, true)
    }

    override suspend fun connect(address: InetSocketAddress) {
        suspendCoroutine<Void> {
            channel.connect(address, it, ContCompletionHandler<Void>())
        }
    }

    override suspend fun close(): Unit = coroutineScope {
        launch(Dispatchers.IO) { channel.close() }
    }

    override suspend fun read(buffer: ByteBuffer) = suspendCoroutine<Int> {
        channel.read(buffer, it, ContCompletionHandler<Int>())
    }

    override suspend fun write(buffer: ByteBuffer) = suspendCoroutine<Int> {
        channel.write(buffer, it, ContCompletionHandler<Int>())
    }
}

private class TCPServerSocket(port: UShort) {
    val channel: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open()
    init {
        channel.bind(InetSocketAddress("0.0.0.0", port.toInt()))
        channel.setOption(SO_REUSEADDR, true)
        channel.setOption(SO_REUSEPORT, true)
    }

    suspend fun accept() = TCPSocket(suspendCoroutine {
        channel.accept(it, ContCompletionHandler<AsynchronousSocketChannel>())
    })
}

private class TCPStream(private val socket: TCPSocket, override val thisNodeId: NodeId) : StreamListNode() {
    private val reader = MessageReader(socket)
    private val writer = MessageWriter(socket)

    var backgroundJob: Job? = null

    suspend fun run() = coroutineScope {
        try {
            launch { writer.run() }
            launch { runReceiveLoop() }
        } catch (e: ClosedChannelException) {
            // It's absolutely ok.
            // We catch it here to make all of the
            // endless loops stop.
        }
    }

    private suspend fun runReceiveLoop() = coroutineScope {
        while (true) {
            val msg = reader.read()
            launch { receive(msg.toByteArray()) } // Async launch not to block the receive process
        }
    }

    override suspend fun send(message: Buffer) {
        writer.channel.send(Message.readFrom(message))
    }

    override suspend fun receive(message: Buffer) {
        if (!receivedNodeId(message))
            child?.receive(message)
    }

    private fun receivedNodeId(message: Buffer): Boolean {
        if (_remoteNodeId == null && message.size == NodeId.sizeInBytes) {
            _remoteNodeId = NodeId(message.toUByteArray())
            _nodeIdHandshakeContinuation?.resume(Unit)
            return true
        }
        return false
    }

    override suspend fun close() {
        performClosure()
        backgroundJob?.cancel()
        socket.close()
    }

    /**
     * When connection is opened our streams
     * exchange their NodeId's. It is done separately
     * because extensions and child streams may use this
     * info during stream extending
     */
    suspend fun performNodeIdHandshake(): Unit = coroutineScope {
        suspendCoroutine {
            _nodeIdHandshakeContinuation = it
            launch { send(thisNodeId.data.toByteArray()) }
        }
    }

    private var _nodeIdHandshakeContinuation: Continuation<Unit>? = null
    private var _remoteNodeId: NodeId? = null

    override val remoteNodeId: NodeId
        get() = _remoteNodeId!!

    override val thisEndpoint: Endpoint by lazy {
        IPEndpoint.toEndpoint(socket.channel.localAddress as InetSocketAddress)
    }

    override val remoteEndpoint: Endpoint by lazy {
        IPEndpoint.toEndpoint(socket.channel.remoteAddress as InetSocketAddress)
    }
}

private fun getRandomPort() =
    Random.nextUInt(1000u, UShort.MAX_VALUE.toUInt()).toUShort()

class TCP(port: UShort = getRandomPort()) : Transport() {
    override val descriptor = TransportDescriptor("TCP", setOf(Fast(), Reliable(), Supports(this)))

    private val listener = TCPServerSocket(port)

    val listenerEndpoint: Endpoint by lazy {
        IPEndpoint.toEndpoint(listener.channel.localAddress as InetSocketAddress)
    }

    override suspend fun init() {
        scope?.launch { runAccept() }
    }

    private suspend fun runAccept() = coroutineScope {
        while (true) {
            val socket = listener.accept()
            launch { processStream(socket) }
        }
    }

    private suspend fun processStream(socket: TCPSocket): TCPStream = coroutineScope {
        val stream = TCPStream(socket, nodeId!!)

        // Separate launch to run message receive process
        // after we setup continuation block
        // otherwise we may receive remote node's NodeId
        // before we are ready for it
        launch { stream.backgroundJob = scope?.launch { stream.run() } }
        stream.performNodeIdHandshake()

        extension?.extendStream(stream)
        stream
    }

    override fun supports(endpoint: Endpoint): Boolean =
        IPEndpoint.isValidEndpoint(endpoint)

    override suspend fun connect(endpoint: Endpoint): Unit = coroutineScope {
        val socket = TCPSocket()
        launch(Dispatchers.IO) {
            socket.channel.bind(listener.channel.localAddress)
        }
        socket.connect(IPEndpoint.fromEndpoint(endpoint))

        val stream = processStream(socket)

        // Connection was init by our side
        stream.performHandshake()
    }
}