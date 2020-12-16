package ru.emkn.p2beer.p2p.network

import kotlinx.coroutines.*
import ru.emkn.p2beer.p2p.*

val p2pScopeFactory
    @ObsoleteCoroutinesApi
    get() = CoroutineScope(newSingleThreadContext("P2P"))

class TransportManager(val peerId: PeerId, val scope: CoroutineScope = p2pScopeFactory) {
    val transportsByName: Map<Name, Transport>
        get() = _transportsMap

    val transports: Collection<Transport>
        get() = _transportsMap.values

    private val _transportsMap: MutableMap<Name, Transport> = mutableMapOf()

    var extension : ExtensionNode? = null
        set(value) {
            for (transport in transports)
                transport.extension = value
            value?.backgroundScope = scope
            scope.launch { value?.init() }
            field = value
        }

    suspend fun registerTransport(transport: Transport) {
        transport.extension = extension
        transport.scope = scope
        transport.peerId = peerId

        _transportsMap[transport.descriptor.name] = transport

        withContext(scope.coroutineContext) {
            transport.init()
        }
    }
}