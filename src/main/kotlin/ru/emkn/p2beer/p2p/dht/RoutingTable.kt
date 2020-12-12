package ru.emkn.p2beer.p2p.dht

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*

import java.util.*

data class Node(val id: NodeId, val endpoint: Endpoint)
typealias KBucket = MutableList<Node>

/**
 * [Whitepaper](https://pdos.csail.mit.edu/~petar/papers/maymounkov-kademlia-lncs.pdf)
 */
class RoutingTable(val thisId: NodeId, val maxKBucketSize: Int = 20) {

    // Filled with the one initial k-bucket
    val buckets = mutableListOf<KBucket>(emptyKBucket())

    private fun findBucket(id: NodeId): KBucket? {
        return if (id != thisId)
            buckets.getOrNull(id lcp thisId) ?: buckets.last()
        else
            null
    }

    fun findNode(id: NodeId): Node? =
        findBucket(id)?.firstOrNull { node -> node.id == id }

    infix fun contains(id: NodeId): Boolean =
        findNode(id) != null

    /**
     * We can split only the last bucket.
     * Nodes with LCP length that equals to (buckets size - 1) remain
     * in the bucket, others are moved into a new bucket.
     *
     * @return true if k-bucket was successfully split.
     */
    private fun splitBucket(bucket: KBucket): Boolean {
        if (bucket != buckets.last() || buckets.size == NodeId.sizeInBits)
            return false

        val (prevBucket, nextBucket) =
            bucket.partition { node -> node.id lcp thisId == buckets.lastIndex }

        buckets.remove(bucket)
        buckets.add(prevBucket.toMutableList())
        buckets.add(nextBucket.toMutableList()) //Note that the last bucket can be empty

        return true
    }

    fun putNode(node: Node) {
        if (contains(node.id))
            return

        var bucket = findBucket(node.id) ?: return

        if (bucket.size == maxKBucketSize) {
            if (splitBucket(bucket))
                bucket = findBucket(node.id)!!
            else
                return
        }

        if (bucket.size < maxKBucketSize)
            bucket.add(node)
    }

    fun findNearestNodes(id: NodeId): List<Node> {
        val bucket = findBucket(id) ?: return emptyList()

        val result = mutableListOf<Node>()
        result.addAll(bucket)

        val cur = buckets.indexOf(bucket)
        var l: Int = cur - 1
        var r: Int = cur + 1
        while (result.size < maxKBucketSize && (l >= 0 || r < bucket.size)) {
            if (l >= 0)
                result.addAll(buckets[l--])
            if (r < bucket.size)
                result.addAll(buckets[r++])
        }

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
            LinkedList<Node>()
    }
}
