package p2p.dht

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.random.Random

import org.junit.jupiter.api.*
import kotlin.test.*

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.dht.*
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.transports.*

private class BasicSetup(port: UShort = 0u, coroutineScope: CoroutineScope = p2pScopeFactory) {
    val peerId = PeerId.random()
    val transportManager = TransportManager(peerId, scope = coroutineScope)
    val tcp = TCP(port)
    val tcpHolePuncher = TCPHolePunchingExtension(tcp)
    val kadDht = KadDHTExtension(peerId, transportManager, tcpHolePuncher)

    val protocolRouter = protocolRouterOf(
        KadDHTExtension.protocolDescriptor to kadDht,
        TCPHolePunchingExtension.protocolDescriptor to tcpHolePuncher
    )

    suspend fun init() {
        transportManager.registerTransport(tcp)
        transportManager.extension = protocolRouter
    }
}

private fun BasicSetup.toPeer() =
    Peer(peerId, tcp.listenerEndpoint)

class KadDhtTests {
    @Test
    fun `test streams to`() = runBlocking {
        val n = 3
        val setup = BasicSetup()
        setup.init()

        val bootstrap = BasicSetup()
        bootstrap.init()

        setup.tcp.connect(bootstrap.tcp.listenerEndpoint)
        val instances = Array(n) {
            val s = BasicSetup(); s.init()
            s.tcp.connect(bootstrap.tcp.listenerEndpoint)
            s
        }

        delay(100)

        withContext(setup.transportManager.scope.coroutineContext) {
            val streams = setup.kadDht.streamsTo(
                peers = instances.map { it.toPeer() }.toSet(),
                mediators = setOf(bootstrap.toPeer())
            )
            assertEquals(instances.size, streams.size)
        }
    }

    @Test
    fun `three nodes test`(): Unit = runBlocking {
        val ours = BasicSetup()
        val bootstrap = BasicSetup()
        val other = BasicSetup()

        ours.init()
        bootstrap.init()
        other.init()

        withContext(ours.transportManager.scope.coroutineContext) {
            ours.kadDht.bootstrap(bootstrap.tcp.listenerEndpoint)
        }
        withContext(other.transportManager.scope.coroutineContext) {
            other.kadDht.bootstrap(bootstrap.tcp.listenerEndpoint)
        }

        withContext(ours.transportManager.scope.coroutineContext) {
            val searchResult = ours.kadDht.findPeer(other.peerId)
            assertEquals(other.peerId, searchResult?.id)
            assertNotNull(searchResult?.stream)
        }
    }

    @Test
    fun `run massive test`() = runBlocking {
        val singleThreadScope = p2pScopeFactory
        val n = 100
        val instances = Array(n) { i ->
            val setup = BasicSetup(port = (1500 + 10 * i).toUShort(), singleThreadScope)
            setup.init()
            setup
        }

        delay(100)

        val b = 1
        val bootstrapNodes = instances.takeLast(b)

        val jobs = instances.take(n - b).map { ss ->
            launch {
                delay(Random.nextLong(3000))
                withContext(ss.transportManager.scope.coroutineContext) {
                    ss.kadDht.bootstrap(
                        bootstrapNodes
                            .filter { peer -> peer.peerId != ss.peerId }
                            .random().tcp.listenerEndpoint
                    )
                }
            }
        }
        joinAll(*jobs.toTypedArray())

        delay(100)
        repeat(20) {
            val dialing = instances.random()

            withContext(dialing.transportManager.scope.coroutineContext) {
                val searchedPeer = instances.filter { it.peerId != dialing.peerId }.random()

                println("Dialing ${dialing.peerId}")
                println("looks for ${searchedPeer.peerId}")
                val peer = dialing.kadDht.findPeer(searchedPeer.peerId, maxIterations = 20)
                assertEquals(searchedPeer.peerId, peer?.id)
            }
        }

        instances.forEach { it.transportManager.scope.cancel() }
    }
}