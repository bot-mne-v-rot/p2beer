package ru.emkn.p2beer.p2p.network

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.Name
import ru.emkn.p2beer.p2p.PeerId
import ru.emkn.p2beer.p2p.network.traits.Trait

import java.io.IOException

data class TransportDescriptor(val name: Name, val traits: Set<Trait>)

typealias Endpoint = String

interface ConnectionStrategy {
    suspend fun connect(endpoint: Endpoint)
}

abstract class Transport {
    abstract val descriptor: TransportDescriptor

    /**
     * Set by [TransportManager].
     *
     * Do not set its parent. Just use it to extend streams.
     */
    var extension: ExtensionNode? = null

    /**
     * Set by [TransportManager].
     */
    var scope: CoroutineScope? = null

    /**
     * Our [PeerId].
     * Set by [TransportManager].
     */
    var peerId: PeerId? = null

    /**
     * Describes if transport can connect to the endpoint
     */
    abstract fun supports(endpoint: Endpoint): Boolean

    abstract suspend fun init()

    /**
     * Initiates new connection then extends new stream
     * and finally calls [StreamNode.performHandshake]
     *
     * @throws IOException
     * @throws HandshakeFailedException
     * @throws IllegalStateException
     */
    abstract suspend fun connect(endpoint: Endpoint)

    open var defaultStrategy: ConnectionStrategy =
        object : ConnectionStrategy {
            override suspend fun connect(endpoint: Endpoint) {
                this@Transport.connect(endpoint)
            }
        }

    open suspend fun connectWithStrategy(endpoint: Endpoint, strategy: ConnectionStrategy) {
        strategy.connect(endpoint)
    }

    suspend fun connectWithDefaultStrategy(endpoint: Endpoint) {
        connectWithStrategy(endpoint, defaultStrategy)
    }
}