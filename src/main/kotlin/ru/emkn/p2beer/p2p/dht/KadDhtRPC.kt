package ru.emkn.p2beer.p2p.dht

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*

import kotlinx.coroutines.*
import com.google.protobuf.ByteString

class KadDhtRPC(send: suspend (Buffer) -> Unit) {
    suspend fun receive(message: Buffer) {
        rpcBase.receive(message)
    }

    suspend fun findPeers(peerId: PeerId): List<Peer> {
        val request = FindPeersRequest(peerId)
        val requestMsg = serializeFindPeersRequest(request)

        val responseMsg = rpcBase.makeRPC(MessageTypes.FIND_PEERS.typeId, requestMsg)
        val response = deserializeFindPeersResponse(responseMsg)
        return response.peers
    }

    fun onFindPeersRequest(handler: suspend (PeerId) -> List<Peer>) {
        rpcBase.rpcHandlers[MessageTypes.FIND_PEERS.typeId] = { requestMsg ->
            val request = deserializeFindPeersRequest(requestMsg)
            val response = FindPeersResponse(handler(request.peerId))
            serializeFindPeersResponse(response)
        }
    }

    private enum class MessageTypes(val typeId: UByte) {
        FIND_PEERS(0u),
    }

    private val rpcBase = RPCBase(send)
}

private data class FindPeersRequest(val peerId: PeerId)

private fun serializeFindPeersRequest(request: FindPeersRequest) =
    KadDHTProtos.FindPeersRequest
        .newBuilder()
        .setPeerId(ByteString.copyFrom(request.peerId.data.toByteArray()))
        .build()
        .toByteArray()

private suspend fun deserializeFindPeersRequest(bytes: ByteArray): FindPeersRequest {
    val proto = withContext(Dispatchers.IO) {
        KadDHTProtos.FindPeersRequest
            .parseFrom(bytes)
    }

    return FindPeersRequest(proto.peerId.toPeerId())
}

private data class FindPeersResponse(val peers: List<Peer>)

private fun serializePeer(peer: Peer) =
    KadDHTProtos.Peer
        .newBuilder()
        .setPeerId(peer.id.toByteString())
        .setEndpoint(peer.endpoint)
        .build()

private fun deserializePeer(proto: KadDHTProtos.Peer) =
    Peer(
        proto.peerId.toPeerId(),
        proto.endpoint
    )

private fun serializeFindPeersResponse(response: FindPeersResponse) =
    KadDHTProtos.FindPeersResponse
        .newBuilder()
        .addAllPeers(response.peers.map { serializePeer(it) })
        .build()
        .toByteArray()

private suspend fun deserializeFindPeersResponse(bytes: ByteArray): FindPeersResponse {
    val proto = withContext(Dispatchers.IO) {
            KadDHTProtos.FindPeersResponse
                .parseFrom(bytes)
        }

    return FindPeersResponse(proto.peersList.map { deserializePeer(it) })
}