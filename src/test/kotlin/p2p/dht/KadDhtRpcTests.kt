package p2p.dht

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.*

import p2p.network.StreamConnector

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.dht.*
import ru.emkn.p2beer.p2p.network.*

private class KadDhtRpcStream : StreamLeafNode() {
    val rpc = KadDhtRPC { send(it) }

    override suspend fun receive(message: Buffer) {
        rpc.receive(message)
    }
}

class KadDhtRpcTests {
    @Test
    fun `test find peers request and response`() = runBlocking {
        val connector = StreamConnector()
        val inner1 = KadDhtRpcStream()
        val inner2 = KadDhtRpcStream()

        connector.left = inner1
        connector.right = inner2

        val expectedResponse = mutableListOf<Peer>()

        repeat(20) {
            expectedResponse.add(Peer(PeerId.random(), endpoint = "a"))
        }

        val requestHandler: suspend (PeerId) -> List<Peer> =
            { _ -> expectedResponse }

        inner2.rpc.onFindPeersRequest(requestHandler)

        val response1 = inner1.rpc.findPeers(PeerId.random())
        assertEquals(expectedResponse, response1)

        // Double check, don't ask me why please.
        // So much labour was put to debug it
        val response2 = inner1.rpc.findPeers(PeerId.random())
        assertEquals(expectedResponse, response2)
    }
}