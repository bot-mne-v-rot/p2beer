package ru.emkn.p2beer.p2p.dht

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*

/**
 * [Whitepaper](https://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf)
 */
interface DHT {
    suspend fun bootstrap(endpoint: Endpoint, timeoutMillis: Long = 1000)

    suspend fun findPeer(id: PeerId, maxIterations: Int = 5): Peer?

    /**
     * Iterative algorithm to find nearest to [id] peers in terms of XOR metric.
     * Cornerstone for other methods to be implemented.
     *
     * @param alpha is widely-used concurrency parameter. See whitepaper
     */
    suspend fun findNearestPeers(
        id: PeerId,
        maxIterations: Int = 5,
        alpha: Int = 3,
        blindSearch: Boolean = false
    ): List<Peer>

    suspend fun get(key: Buffer): Buffer

    suspend fun put(key: Buffer, value: Buffer)
}