package ru.emkn.p2beer.p2p.network.transports.messaging

import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun putInt(value: Int, byteBuffer: ByteBuffer) {
    byteBuffer.order(ByteOrder.BIG_ENDIAN).putInt(value)
}

internal fun getInt(byteBuffer: ByteBuffer) : Int =
    byteBuffer.order(ByteOrder.BIG_ENDIAN).int

class Message() {
    val complete: Boolean
        get() = remaining == 0

    // -1 means that we got no initial packet so far
    var size: Int = -1
        private set

    val position: Int
        get() = buffer.position()

    val remaining: Int
        get() = size - position

    internal var buffer: ByteBuffer = ByteBuffer.allocate(256)

    fun append(bytes: ByteBuffer) {
        if (size == -1)
            readSize(bytes)

        val bytesToCopy = min(bytes.remaining(), remaining)
        if (bytesToCopy > buffer.remaining())
            expandBuffer()

        // Unfortunately, Java implementation is no faster
        repeat(bytesToCopy) {
            buffer.put(bytes.get())
        }
    }

    private fun readSize(bytes: ByteBuffer) {
        if (bytes.remaining() < Int.SIZE_BYTES)
            throw IllegalArgumentException("Initial packet contains no encoded size")
        size = getInt(bytes)
        if (size < 0 || size > MAX_SIZE)
            throw IllegalArgumentException("Size is invalid")
    }

    private fun expandBuffer() {
        // We don't trust the channel so we increase size of the buffer
        // only when necessary
        val newBuffer = ByteBuffer.allocate(buffer.capacity() * 2)
        buffer.flip()
        newBuffer.put(buffer)
        buffer = newBuffer
    }

    companion object {
        const val MAX_SIZE = 1024 * 1024 * 1024 // One GiB

        fun readFrom(array: ByteArray): Message {
            if (array.size > MAX_SIZE)
                throw IllegalArgumentException("Size is invalid")

            val message = Message()
            message.buffer = ByteBuffer.allocate(array.size + Int.SIZE_BYTES)

            putInt(array.size, message.buffer)
            message.buffer.put(array)

            return message
        }
    }
}

/**
 * Returns internal buffer as Read-Only
 * After calling the function
 * the message is invalidated and doing
 * anything with it leads to undefined
 * behavior.
 */
fun Message.toByteBuffer(): ByteBuffer {
    buffer.flip()
    return buffer
}

fun Message.toByteArray(): ByteArray {
    val array = ByteArray(size)
    buffer.array().copyInto(array, endIndex = size)
    return array
}