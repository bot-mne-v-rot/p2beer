package ru.emkn.p2beer.p2p.dht

import kotlinx.coroutines.*
import java.util.*

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*
import ru.emkn.p2beer.p2p.network.traits.*
import ru.emkn.p2beer.p2p.network.transports.*
import java.lang.ref.WeakReference
import java.nio.channels.AlreadyConnectedException

class KadDHTExtension(
    private val thisId: PeerId,
    private val transportManager: TransportManager,
    private val tcpHolePuncher: TCPHolePunchingExtension
) : DHT, ExtensionLeafNode() {

    // It also tries to connect to the peer
    override suspend fun findPeer(id: PeerId, maxIterations: Int): Peer? {
        val nearest = findNearestPeers(id, maxIterations)
        val peer = nearest.firstOrNull { peer -> peer.id == id }
        if (peer != null) {
            val stream =
                streamsTo(
                    peers = setOf(peer),
                    nearest.filter { it.id != peer.id }.toSet()
                ).firstOrNull()
            return Peer(
                id = peer.id,
                endpoint = stream?.remoteEndpoint ?: peer.endpoint,
                stream = stream
            )
        }
        return null
    }

    override suspend fun findNearestPeers(
        id: PeerId,
        maxIterations: Int,
        alpha: Int,
        blindSearch: Boolean
    ): List<Peer> {
        val comparator = Comparator<Peer> { n1, n2 -> id.xorCmp(n1.id, n2.id) }

        val candidates: MutableSet<Peer> = TreeSet(comparator)
        val askedCandidates: MutableSet<Peer> = TreeSet(comparator)
        val askedPeersIds: MutableSet<PeerId> = TreeSet { id1, id2 -> id.xorCmp(id1, id2) }

        candidates.addAll(routingTable.findNearestPeers(id))

        for (iter in 0 until maxIterations) {
            findNearestPeersIteration(
                id, blindSearch, alpha,
                candidates, askedCandidates,
                askedPeersIds
            )
        }

        return (candidates + askedCandidates)
            .take(routingTable.maxKBucketSize)
            .toList()
    }

    private suspend fun findNearestPeersIteration(
        id: PeerId,
        blindSearch: Boolean,
        alpha: Int,
        candidates: MutableSet<Peer>,
        askedCandidates: MutableSet<Peer>,
        askedPeersIds: MutableSet<PeerId>
    ) {
        if (!blindSearch && candidates.any { n -> n.id == id })
            return

        println(candidates)

        val toAsk = candidates.take(alpha).toSet()
        candidates.removeAll(toAsk)
        askedCandidates.addAll(toAsk)
        askedPeersIds.addAll(toAsk.map { it.id })

        val mediators = (candidates + askedCandidates)

        val streams = streamsTo(toAsk, mediators)

        askPeersAboutId(streams, id, candidates, askedPeersIds)

        closeUnusedStreams(streams)
    }

    private suspend fun closeUnusedStreams(streams: List<KadDHTStream>) {
        streams.forEach {
            if (routingTable.findPeer(it.remotePeerId) == null) {
                streamStore.remove(it.remotePeerId)
                it.close()
            }
        }
    }

    private suspend fun askPeersAboutId(
        streams: List<KadDHTStream>,
        id: PeerId,
        result: MutableSet<Peer>,
        askedPeersIds: MutableSet<PeerId>
    ) {
        supervisorScope {
            result.addAll(
                streams.map {
                    async { it.rpc.findPeers(id) }
                }
                    .awaitAll()
                    .flatten()
                    .filter {
                        it.id != thisId && it.id !in askedPeersIds
                    }
                    .groupBy { it.id }.values
                    .map { peersWithSameId ->
                        peersWithSameId.first()
                    }
            )
        }
    }

    suspend fun streamsTo(
        peers: Set<Peer>,
        mediators: Set<Peer>
    ): List<KadDHTStream> = supervisorScope {
        peers
            .map { it.id }
            .map { id ->
                async {
                    streamStore[id]?.let {
                        if (it.opened)
                            return@async it
                    }

                    try {
                        connectTo(id, mediators)
                    } catch (e: ConnectionFailedException) {
                        // OK
                    } catch (e: AlreadyConnectedException) {
                        println("Already connected exception occurred: $e")
                    }

                    streamStore[id]
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun connectTo(
        remotePeerId: PeerId,
        mediators: Set<Peer>,
        maxMediators: Int = 10
    ) {
        val mediatorsIds = mediators
            .map { it.id }
            .sortedWith { id1, id2 -> remotePeerId.xorCmp(id1, id2) }

        tcpHolePuncher.connectTo(remotePeerId, mediatorsIds, maxMediators)
    }

    override suspend fun bootstrap(endpoint: Endpoint, timeoutMillis: Long) {
        val transport =
            transportManager.transports
                .filter(reliable and supports(endpoint))
                .firstOrNull() ?: return

        withTimeout(timeoutMillis) {
            transport.connect(endpoint)
        }

        findNearestPeers(thisId, blindSearch = true)

        println(routingTable.buckets.flatten().size)
    }

    override suspend fun get(key: Buffer): Buffer {
        TODO("Not yet implemented")
    }

    override suspend fun put(key: Buffer, value: Buffer) {
        TODO("Not yet implemented")
    }

    override suspend fun init() = Unit

    override suspend fun extendStream(node: StreamListNode) {
        val stream = KadDHTStream(routingTable)
        node.child = stream

        streamStore[stream.remotePeerId] = stream
        routingTable.putPeer(Peer(stream.remotePeerId, stream.remoteEndpoint, stream))
    }

    // Weak hash map is used not to prevent garbage collector
    // do its job of taking our dead streams.
    private val streamStore = WeakHashMap<PeerId, KadDHTStream>()

    private val routingTable = RoutingTable(thisId)

    companion object {
        private val currentVersion = ProtocolVersion(1u, 0u, 0u)
        private val leastSupportedVersion = currentVersion
        val protocolDescriptor =
            ProtocolDescriptor(
                name = "KadDHT",
                currentVersion,
                leastSupportedVersion
            )
    }
}

class KadDHTStream(private val routingTable: RoutingTable) : StreamLeafNode() {
    val rpc = KadDhtRPC { send(it) }

    init {
        rpc.onFindPeersRequest(this::onFindPeersRequest)
    }

    private fun onFindPeersRequest(peerId: PeerId) =
        routingTable.findNearestPeers(peerId)

    override suspend fun receive(message: Buffer) {
        rpc.receive(message)
    }
}
