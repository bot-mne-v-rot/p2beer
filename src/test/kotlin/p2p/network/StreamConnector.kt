package p2p.network

import org.junit.jupiter.api.*
import io.mockk.*
import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.Buffer

internal class PerStreamConnector(private val biConnector: StreamConnector) : StreamListNode() {
    override suspend fun send(message: Buffer) {
        biConnector.send(message, this)
    }
}

/**
 * Emulates transport. Useful for testing purposes
 */
class StreamConnector {
    private val leftConnector = PerStreamConnector(this)
    private val rightConnector = PerStreamConnector(this)

    var left: StreamNode? = null
        set(value) {
            leftConnector.child = value
            field = value
        }

    var right: StreamNode? = null
        set(value) {
            rightConnector.child = value
            field = value
        }

    internal suspend fun send(message: Buffer, stream: PerStreamConnector) {
        if (leftConnector != stream)
            left?.receive(message)
        else if (rightConnector != stream)
            right?.receive(message)
    }
}

class StreamConnectorTests {
    @Test
    fun `test communication`() = runBlocking {
        val connector = StreamConnector()

        val inter1 = StreamListNode()
        val inter2 = StreamListNode()

        val mock1 = mockk<StreamLeafNode>()
        val mock2 = mockk<StreamLeafNode>()

        val assemble = { mock: StreamLeafNode, inter: StreamListNode ->
            coEvery { mock.receive(any()) } returns Unit
            every { mock.parent = any() } returns Unit

            inter.child = mock
        }

        assemble(mock1, inter1)
        assemble(mock2, inter2)

        connector.left = inter1
        connector.right = inter2

        val msgA = "A".toByteArray()
        val msgB = "B".toByteArray()

        inter1.send(msgA)
        coVerify { mock2.receive(msgA) }

        inter2.send(msgB)
        coVerify { mock1.receive(msgB) }
    }
}