package p2p.network.transports.messaging

import org.junit.jupiter.api.*
import kotlin.test.*

import kotlinx.coroutines.*

import java.nio.ByteBuffer
import kotlin.random.*
import java.lang.Integer.min

import ru.emkn.p2beer.p2p.network.transports.messaging.*

class MessageTests {
    @Test
    fun `put and get UInt in and from ByteBuffer`() {
        val buffer = ByteBuffer.wrap(ByteArray(50))

        val a = Random.nextInt()
        putInt(a, buffer)

        buffer.flip()
        val b = getInt(buffer)

        assertEquals(a, b)
        assertEquals(0, buffer.remaining())
    }

    @Test
    fun `test message construction from pieces`() {
        val origArray = Random.nextBytes(1000)
        val origMessage = Message.readFrom(origArray)
        val origBuffer = origMessage.toByteBuffer()

        val message = Message()

        val smallBuffer = ByteBuffer.allocate(50)
        while (origBuffer.hasRemaining()) {
            smallBuffer.clear()
            repeat(min(origBuffer.remaining(), smallBuffer.remaining())) {
                smallBuffer.put(origBuffer.get())
            }
            smallBuffer.flip()
            message.append(smallBuffer)
        }

        val newArray = message.toByteArray()
        assert(origArray.contentEquals(newArray))
    }
}