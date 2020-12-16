package p2p.dht

import org.junit.jupiter.api.*
import kotlin.test.*

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.dht.*
import ru.emkn.p2beer.p2p.network.Endpoint


class RoutingTableTests {
    @Test
    fun `test bucket distribution`() {
        val thisId = PeerId.random()
        val routingTable = RoutingTable(thisId, maxKBucketSize = 3)

        repeat(20) {
            routingTable.putPeer(Peer(PeerId.random(), Endpoint()))
        }

        routingTable.buckets.forEachIndexed { index, bucket ->
            bucket.forEach { node ->
                val lcp = node.id lcp thisId
                if (index == routingTable.buckets.lastIndex)
                    assertTrue(index <= lcp)
                else
                    assertEquals(index, lcp)
            }
        }
    }

    @Test
    fun `test finding nearest`() = repeat(100) {
        val thisId = PeerId.zeroes()
        val maxKBucketSize = 3
        val routingTable = RoutingTable(thisId, maxKBucketSize)

        repeat(20) {
            routingTable.putPeer(Peer(PeerId.random(), Endpoint()))
        }

        val desiredNode = PeerId.random()

        val nearest = routingTable.findNearestPeers(desiredNode)

        val expected = routingTable.buckets
            .flatten()
            .sortedWith { n1, n2 -> desiredNode.xorCmp(n1.id, n2.id) }
            .take(maxKBucketSize)

        assertEquals(expected, nearest)
    }
}