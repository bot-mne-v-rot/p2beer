package ru.emkn.p2beer.p2p.network.transports.messaging

import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

class MessageReader(private val socket: Socket, bufferSize: Int = 256) {
    private val buffer = ByteBuffer.allocate(bufferSize)
    private var current = Message()

    suspend fun read(): Message {
        while (!current.complete) {
            buffer.clear()
            val readBytes = socket.read(buffer)

            // It's ok we propagate the exception
            if (readBytes == -1)
                throw ClosedChannelException()

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