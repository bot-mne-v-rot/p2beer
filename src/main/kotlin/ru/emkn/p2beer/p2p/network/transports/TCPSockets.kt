package ru.emkn.p2beer.p2p.network.transports

import ru.emkn.p2beer.p2p.network.transports.messaging.*

import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.nio.ByteBuffer
import java.net.InetSocketAddress
import java.net.StandardSocketOptions.*

import kotlin.coroutines.*
import kotlinx.coroutines.*

internal class ContCompletionHandler<T> : CompletionHandler<T, CancellableContinuation<T>> {
    override fun completed(result: T, attachment: CancellableContinuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) {
        attachment.resumeWithException(exc)
    }
}

internal class TCPSocket(val channel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()) : Socket {
    init {
        channel.setOption(SO_REUSEADDR, true)
        channel.setOption(SO_REUSEPORT, true)
        channel.setOption(SO_KEEPALIVE, true)
    }

    override suspend fun bind(localAddress: InetSocketAddress): Unit =
        withContext(Dispatchers.IO) { channel.bind(localAddress) }

    override suspend fun connect(address: InetSocketAddress) {
        suspendCancellableCoroutine<Void> {
            it.invokeOnCancellation { channel.close() }
            channel.connect(address, it, ContCompletionHandler<Void>())
        }
    }

    override suspend fun close(): Unit =
        withContext(Dispatchers.IO) { channel.close() }

    override suspend fun read(buffer: ByteBuffer) = suspendCancellableCoroutine<Int> {
        channel.read(buffer, it, ContCompletionHandler<Int>())
    }

    override suspend fun write(buffer: ByteBuffer) = suspendCancellableCoroutine<Int> {
        channel.write(buffer, it, ContCompletionHandler<Int>())
    }
}

internal class TCPServerSocket {
    val channel: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open()

    init {
        channel.setOption(SO_REUSEADDR, true)
        channel.setOption(SO_REUSEPORT, true)
    }

    suspend fun bind(localAddress: InetSocketAddress): Unit =
        withContext(Dispatchers.IO) { channel.bind(localAddress) }

    suspend fun bind(port: UShort) =
        bind(InetSocketAddress(port.toInt()))

    suspend fun accept() = TCPSocket(suspendCancellableCoroutine {
        channel.accept(it, ContCompletionHandler<AsynchronousSocketChannel>())
    })

    suspend fun close() =
        withContext(Dispatchers.IO) { channel.close() }
}