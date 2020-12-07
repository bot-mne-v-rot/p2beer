package ru.emkn.p2beer.p2p.network

import kotlinx.coroutines.*
import ru.emkn.p2beer.p2p.*

val p2pContextFactory
    @ObsoleteCoroutinesApi
    get() = CoroutineScope(newSingleThreadContext("P2P"))

class TransportManager(val nodeId: NodeId, val scope: CoroutineScope = p2pContextFactory) {
    val transportsByName: Map<Name, Transport>
        get() = _transportsMap

    val transports: Collection<Transport>
        get() = _transportsMap.values

    private val _transportsMap: MutableMap<Name, Transport> = mutableMapOf()

    var extension : ExtensionNode? = null
        set(value) {
            for (transport in transports)
                transport.extension = value
            field = value
            scope.launch { extension?.init() }
        }

    fun registerTransport(transport: Transport) {
        transport.extension = extension
        transport.scope = scope
        transport.nodeId = nodeId

        _transportsMap[transport.descriptor.name] = transport

        runBlocking {
            val job = scope.launch { transport.init() }
            job.join()
        }
    }
}