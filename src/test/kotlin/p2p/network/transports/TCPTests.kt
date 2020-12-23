package p2p.network.transports

import org.junit.jupiter.api.*
import io.mockk.*

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.PeerId
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.transports.TCP
import kotlin.random.Random
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
        suspend fun setup(manager: TransportManager, transport: TCP, inter: StreamListNode, mock: StreamListNode) {
            manager.registerTransport(transport)
            val ext1 = AutoAttachExtension(inter)
            val ext2 = AutoAttachExtension(mock)
            ext1.child = ext2
            manager.extension = ext1
        }

        val peerId1 = PeerId.random()
        val peerId2 = PeerId.random()

        val manager1 = TransportManager(peerId1, scope)
        val manager2 = TransportManager(peerId2, scope)

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

        delay(10) // Waiting for both sides to receive peerIds

        assertEquals(peerId1, inter1.thisPeerId)
        assertEquals(peerId2, inter2.thisPeerId)
        assertEquals(peerId2, inter1.remotePeerId)
        assertEquals(peerId1, inter2.remotePeerId)

        withContext(scope.coroutineContext) {
            coVerify { mock1.performHandshake() }

            repeat(100) {
                val msgA = Random.Default.nextBytes(2000)
                inter1.send(msgA)
                delay(20) // Waiting for our message to be sent
                coVerify { mock2.receive(msgA) }

                val msgB = Random.Default.nextBytes(2000)
                inter2.send(msgB)
                delay(20) // Waiting for our message to be sent
                coVerify { mock1.receive(msgB) }
            }

            inter1.close()

            coVerify { mock1.performClosure() }
        }

        // Waiting for both sides to close
        delay(50)

        scope.cancel()
    }
}