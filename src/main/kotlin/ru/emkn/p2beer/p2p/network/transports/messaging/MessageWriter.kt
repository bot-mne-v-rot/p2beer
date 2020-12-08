package ru.emkn.p2beer.p2p.network.transports.messaging

import kotlinx.coroutines.channels.*

class MessageWriter(private val socket: Socket) {
    val channel: SendChannel<Message>
        get() = queue

    private val queue = Channel<Message>()

    suspend fun run() {
        for (message in queue) {
            val buffer = message.toByteBuffer()
            while (buffer.hasRemaining())
                socket.write(buffer)
        }
    }
}