package ru.emkn.p2beer.p2p.dht

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*

import java.util.*

data class Peer(
    val id: PeerId,
    val endpoint: Endpoint = Endpoint(),
    val stream: StreamNode? = null
)
typealias KBucket = MutableList<Peer>

/**
 * [Whitepaper](https://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf)
 */
class RoutingTable(val thisId: PeerId, val maxKBucketSize: Int = 20) {

    // Filled with the one initial k-bucket
    val buckets = mutableListOf<KBucket>(emptyKBucket())

    private fun findBucket(id: PeerId): KBucket =
        buckets.getOrNull(id lcp thisId) ?: buckets.last()

    fun findPeer(id: PeerId): Peer? =
        findBucket(id).firstOrNull { node -> node.id == id }

    infix fun contains(id: PeerId): Boolean =
        findPeer(id) != null

    /**
     * We can split only the last bucket.
     * Nodes with LCP length that equals to (buckets size - 1) remain
     * in the bucket, others are moved into a new bucket.
     *
     * @return true if k-bucket was successfully split.
     */
    private fun splitBucket(bucket: KBucket): Boolean {
        if (bucket != buckets.last() || buckets.size == PeerId.sizeInBits)
            return false

        val (prevBucket, nextBucket) =
            bucket.partition { node -> node.id lcp thisId == buckets.lastIndex }

        buckets.remove(bucket)
        buckets.add(prevBucket.toMutableList())
        buckets.add(nextBucket.toMutableList()) //Note that the last bucket can be empty

        return true
    }

    /**
     * @return true if node was successfully put
     */
    fun putPeer(peer: Peer): Boolean {
        if (contains(peer.id))
            return false

        var bucket = findBucket(peer.id)

        if (bucket.size == maxKBucketSize) {
            if (splitBucket(bucket))
                bucket = findBucket(peer.id)
            else
                return false
        }

        // Bad luck
        if (bucket.size == maxKBucketSize)
            return false

        bucket.add(peer)

        return true
    }

    fun findNearestPeers(id: PeerId): List<Peer> {
        val bucket = findBucket(id)

        val result = mutableListOf<Peer>()
        result.addAll(bucket)

        val cur = buckets.indexOf(bucket)

        if (result.size < maxKBucketSize) {
            var r: Int = cur + 1
            while (r < buckets.size)
                result.addAll(buckets[r++])
        }

        var l: Int = cur - 1
        while (result.size < maxKBucketSize && l >= 0)
            result.addAll(buckets[l--])

        return result
            .sortedWith { n1, n2 -> id.xorCmp(n1.id, n2.id) }
            .take(maxKBucketSize)
    }

    override fun toString(): String {
        return buckets.joinToString("\n") { bucket ->
            "Bucket:\n" +
                    bucket.joinToString("\n") { node ->
                        """|- Id: ${node.id.toStringBin()}
                           |  Endpoint: ${node.endpoint}
                        """.trimMargin()
                    }
        }
    }

    companion object {
        private fun emptyKBucket() =
            LinkedList<Peer>()
    }
}
