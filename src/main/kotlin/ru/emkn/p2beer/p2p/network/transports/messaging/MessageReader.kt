package ru.emkn.p2beer.p2p.network.transports.messaging

import java.nio.ByteBuffer

class MessageReader(private val socket: Socket, bufferSize: Int = 256) {
    private val buffer = ByteBuffer.allocate(bufferSize)
    private var current = Message()

    suspend fun read(): Message {
        while (!current.complete) {
            buffer.clear()
            socket.read(buffer)
            buffer.flip()
            current.append(buffer)
        }
        val result = current

        current = Message()
        if (buffer.hasRemaining())
            current.append(buffer)

        return result
    }
}