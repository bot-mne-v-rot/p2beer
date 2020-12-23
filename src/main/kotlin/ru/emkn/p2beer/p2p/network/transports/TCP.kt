package ru.emkn.p2beer.p2p.network.transports

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.traits.*
import ru.emkn.p2beer.p2p.network.transports.messaging.*

import java.nio.channels.*
import java.net.InetSocketAddress
import java.io.IOException

import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.random.*

private class TCPStream(
    private val socket: TCPSocket,
    override val thisPeerId: PeerId,
    override val transport: TransportDescriptor
) : StreamListNode() {

    private val reader = MessageReader(socket)
    private val writer = MessageWriter(socket)

    var backgroundIOJob: Job? = null

    suspend fun runIO() =
        try {
            /**
             * [coroutineScope] is used explicitly
             * instead of [supervisorScope] to stop
             * all the children on exceptions.
             */
            coroutineScope {
                launch { writer.run() }
                launch { runReceiveLoop() }
            }
        } catch (e: ClosedChannelException) {
            // It's absolutely ok.
            // We catch it here to make all of the
            // endless loops stop.
        } catch (e: IOException) {
            // TODO: Setup logging
            println("Oooo MOYA OBORONA")
        } finally {
            if (socket.channel.isOpen)
                socket.close()
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
        try {
            child?.receive(message)
        } catch (e: Throwable) {
            /**
             * We ignore all the errors because there
             * is no one further to process them.
             *
             * Ideally, here should be some analyzer
             * to close the connection if too many
             * exceptions occur which means that
             * we are receiving too many incorrect
             * messages.
             */
            println(e)
        }
    }

    /**
     * Actually, there is no precise way to determine if the
     * incoming PeerId is correct so we do only message length
     * check.
     *
     * @return true if the incoming message seems to be PeerId
     */
    private suspend fun receivePeerIdOrClose(messageBytes: Buffer): Boolean {
        return if (_remotePeerId == null && messageBytes.size == PeerId.sizeInBytes) {
            _remotePeerId = PeerId(data = messageBytes.toUByteArray())
            true
        } else {
            close()
            false
        }
    }

    /**
     * Close the connection.
     * It will succeed even if something happens.
     */
    override suspend fun close() {
        performClosure()
        backgroundIOJob?.cancel()
        socket.close()
    }

    /**
     * When connection is opened our streams
     * exchange their PeerId's. It is done separately
     * because extensions and child streams may use this
     * info during stream extending
     */
    suspend fun performPeerIdHandshake() {
        sendOurPeerId()
        receiveRemotePeerId()
        closeIfConnectedToItself()
    }

    private suspend fun sendOurPeerId() {
        writer.write(Message.readFrom(thisPeerId.data.toByteArray()))
    }

    private suspend fun receiveRemotePeerId() {
        receivePeerIdOrClose(reader.read().toByteArray())
    }

    private suspend fun closeIfConnectedToItself() {
        if (thisPeerId == remotePeerId)
            socket.close()
    }

    private var _remotePeerId: PeerId? = null

    override val remotePeerId: PeerId
        get() = _remotePeerId!!

    override val thisEndpoint: Endpoint by lazy {
        (socket.channel.localAddress as InetSocketAddress).toEndpoint()
    }

    override val remoteEndpoint: Endpoint by lazy {
        (socket.channel.remoteAddress as InetSocketAddress).toEndpoint()
    }
}

class TCP(private val listenerPort: UShort = 0u) : Transport() {
    override val descriptor = TransportDescriptor(
        name = "TCP",
        traits = setOf(Fast(), Reliable(), Supports(this))
    )

    private val listener = TCPServerSocket()

    val listenerEndpoint: Endpoint by lazy {
        (listener.channel.localAddress as InetSocketAddress).toEndpoint()
    }

    override suspend fun init() {
        listener.bind(listenerPort)
        scope?.launch { runAccept() }
    }

    private suspend fun runAccept() = coroutineScope {
        while (true) {
            val socket = listener.accept()
            launch { processStream(socket) }
        }
    }

    private suspend fun processStream(socket: TCPSocket): TCPStream = coroutineScope {
        val stream = TCPStream(socket, peerId!!, descriptor)

        stream.performPeerIdHandshake()
        stream.backgroundIOJob = scope?.launch { stream.runIO() }

        extension?.extendStream(stream)
        stream
    }

    override fun supports(endpoint: Endpoint): Boolean =
        endpoint.isValidIPEndpoint()

    suspend fun rawConnect(
        remoteAddress: InetSocketAddress,
        localAddress: InetSocketAddress? = null,
        performHandshake: Boolean = true
    ) {
        val socket = TCPSocket()
        if (localAddress != null)
            socket.bind(localAddress)

        socket.connect(remoteAddress)

        val stream = processStream(socket)
        if (performHandshake)
            stream.performHandshake()
    }

    suspend fun rawAccept(
        localAddress: InetSocketAddress,
        performHandshake: Boolean = true
    ) {
        val serverSocket = TCPServerSocket()
        serverSocket.bind(localAddress)

        val socket = serverSocket.accept()

        val stream = processStream(socket)
        if (performHandshake)
            stream.performHandshake()

        serverSocket.close()
    }

    override suspend fun connect(endpoint: Endpoint) {
        try {
            rawConnect(endpoint.toInetSocketAddress())
        } catch (e: Exception) {
            throw ConnectionFailedException(endpoint, e)
        }
    }

    private fun getRandomPort() =
        Random.nextInt(10000, UShort.MAX_VALUE.toInt())

    companion object {
        /**
         * May seem to be not the best solution.
         * But there is no other way to find free
         * port except opening socket with special
         * options and waiting for OS to allocate an
         * unused port.
         */
        suspend fun unusedEndpoint(): Endpoint {
            val socket = TCPServerSocket()
            socket.bind(port = 0u)

            val endpoint = (socket.channel.localAddress as InetSocketAddress).toEndpoint()
            socket.close()
            return endpoint
        }
    }
}