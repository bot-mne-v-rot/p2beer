package p2p.network.transports

import org.junit.jupiter.api.*
import io.mockk.*

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.PeerId
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.transports.*

import kotlin.random.Random
import kotlin.test.*

private class BasicSetup() {
    val peerId = PeerId.random()
    val transportManager = TransportManager(peerId)
    val tcp = TCP()
    val tcpHolePuncher = TCPHolePunchingExtension(tcp)
}

class TCPHolePunchingTests {
    @Test
    fun `test connection`() = runBlocking {
        suspend fun createSetup(): BasicSetup {
            val setup = BasicSetup()

            withContext(setup.transportManager.scope.coroutineContext) {
                setup.transportManager.registerTransport(setup.tcp)
                setup.transportManager.extension = setup.tcpHolePuncher
            }

            return setup
        }

        val ours = createSetup()
        val other = createSetup()
        val mediator = createSetup()

        val oursContext = ours.transportManager.scope.coroutineContext
        val otherContext = ours.transportManager.scope.coroutineContext

        withContext(oursContext) {
            ours.tcp.connect(mediator.tcp.listenerEndpoint)
        }
        withContext(otherContext) {
            other.tcp.connect(mediator.tcp.listenerEndpoint)
        }

        withContext(oursContext) {
            // There is no assertDoesNotThrow for suspend functions
            ours.tcpHolePuncher.connectTo(other.peerId, listOf(mediator.peerId))
        }
    }
}