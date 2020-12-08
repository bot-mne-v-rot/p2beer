package p2p.network.transports

import org.junit.jupiter.api.*
import io.mockk.*

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.NodeId
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.transports.TCP
import kotlin.test.assertEquals

private class AutoAttachExtension(private val stream: StreamListNode) : ExtensionListNode() {
    override suspend fun extendStream(node: StreamListNode) {
        node.child = stream
        child?.extendStream(stream)
    }
}

class TCPTests {
    @Test
    fun `test workflow`() = runBlocking {
        val scope = p2pScopeFactory

        fun setupMock(mock: StreamListNode) {
            coEvery { mock.receive(any()) } returns Unit
            coEvery { mock.performHandshake() } returns Unit
            coEvery { mock.performClosure() } returns Unit
            every { mock.parent = any() } returns Unit
        }
        fun setup(manager: TransportManager, transport: TCP, inter: StreamListNode, mock: StreamListNode) {
            manager.registerTransport(transport)
            val ext1 = AutoAttachExtension(inter)
            val ext2 = AutoAttachExtension(mock)
            ext1.child = ext2
            manager.extension = ext1
        }

        val nodeId1 = NodeId.random()
        val nodeId2 = NodeId.random()

        val manager1 = TransportManager(nodeId1, scope)
        val manager2 = TransportManager(nodeId2, scope)

        val tcp1 = TCP()
        val tcp2 = TCP()

        val inter1 = StreamListNode()
        val inter2 = StreamListNode()

        val mock1 = mockk<StreamListNode>()
        val mock2 = mockk<StreamListNode>()

        setupMock(mock1)
        setupMock(mock2)

        setup(manager1, tcp1, inter1, mock1)
        setup(manager2, tcp2, inter2, mock2)

        withTimeout(1000) {
            withContext(scope.coroutineContext) {
                tcp1.connect(tcp2.listenerEndpoint)
            }
        }

        delay(10) // Waiting for both sides to receive nodeIds

        assertEquals(nodeId1, inter1.thisNodeId)
        assertEquals(nodeId2, inter2.thisNodeId)
        assertEquals(nodeId2, inter1.remoteNodeId)
        assertEquals(nodeId1, inter2.remoteNodeId)

        withContext(scope.coroutineContext) {
            coVerify { mock1.performHandshake() }

            val msgA = "A".toByteArray()
            inter1.send(msgA)
            delay(20) // Waiting for our message to be sent
            coVerify { mock2.receive(msgA) }

            inter1.close()

            coVerify { mock1.performClosure() }
        }

        // Waiting for both sides to close
        delay(50)

        scope.cancel()
    }
}