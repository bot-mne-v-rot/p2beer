package ru.emkn.p2beer.p2p.network.transports

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.*
import ru.emkn.p2beer.p2p.network.*

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.AlreadyConnectedException
import java.util.*

class TCPHolePunchingExtension(
    val transport: TCP,
    private val connectionAttempts: Int = 20
) : ExtensionLeafNode() {
    override suspend fun extendStream(node: StreamListNode) {
        if (node.transport != transport.descriptor)
            return

        val stream = TCPHolePunchingStream()
        node.child = stream

        stream.onConnectionFirstStepRequest(::onConnectionFirstStepRequest)
        stream.onConnectionSecondStepRequest(::onConnectionSecondStepRequest)

        streamStore[node.remotePeerId] = stream
    }

    /**
     * Opens connection to [remotePeerId]
     * using hole punching technique.
     *
     * @param mediatorsPeerIds mediator candidates.
     * Algorithm tries each candidate one by one.
     * Mediators should be sorted by priority.
     * The first one is the most prioritized.
     *
     * @throws ConnectionFailedException
     * @throws AlreadyConnectedException
     */
    suspend fun connectTo(
        remotePeerId: PeerId,
        mediatorsPeerIds: List<PeerId>,
        maxMediators: Int = 10,
        rpcTimeoutMillis: Long = 1000,
        connectionTimeoutMillis: Long = 1000
    ) {
        println("Connecting to $remotePeerId")

        streamStore[remotePeerId]?.let {
            if (it.opened)
                throw AlreadyConnectedException()
        }

        mediatorsPeerIds
            .filter { id -> streamStore.containsKey(id) }
            .take(maxMediators)
            .firstOrNull { mediatorPeerId ->
                connectTo(
                    remotePeerId,
                    mediatorPeerId,
                    rpcTimeoutMillis,
                    connectionTimeoutMillis
                )
            } ?: throw ConnectionFailedException(remotePeerId)
    }

    /**
     * Connection through concrete mediator
     *
     * @return true if successfully connected
     */
    private suspend fun connectTo(
        remotePeerId: PeerId,
        mediatorPeerId: PeerId,
        rpcTimeoutMillis: Long = 1000,
        connectionTimeoutMillis: Long = 1000
    ): Boolean {
        val streamToMediator = streamStore[mediatorPeerId] ?: return false
        val thisEndpoint = TCP.unusedEndpoint()

        try {
            val remoteEndpoint =
                streamToMediator.performConnectionFirstStep(
                    remotePeerId,
                    thisEndpoint,
                    rpcTimeoutMillis
                ) ?: return false

            return attemptConnect(
                thisEndpoint,
                remoteEndpoint,
                remotePeerId,
                performHandshake = true,
                attemptTimeoutMillis = connectionTimeoutMillis
            )
        } catch (e: TimeoutCancellationException) {
            println("Connection to $remotePeerId timed out")
            return false
        }
    }

    private suspend fun onConnectionFirstStepRequest(
        firstPeerId: PeerId,
        firstEndpoint: Endpoint,
        secondPeerId: PeerId
    ): Endpoint? {
        val streamToSecond = streamStore[secondPeerId] ?: return null
        return streamToSecond.performConnectionSecondStep(
            firstPeerId,
            firstEndpoint,
            rpcTimeoutMillis = 1000
        )
    }

    private suspend fun onConnectionSecondStepRequest(
        firstPeerId: PeerId,
        firstEndpoint: Endpoint
    ): Endpoint {
        val secondEndpoint = TCP.unusedEndpoint()
        println(firstEndpoint + secondEndpoint)

        backgroundScope?.launch {
            val connJob = this.coroutineContext.job
            launch {
                attemptConnect(
                    thisEndpoint = secondEndpoint,
                    remoteEndpoint = firstEndpoint,
                    remotePeerId = firstPeerId,
                    performHandshake = false,
                    anotherConnectionJob = connJob
                )
            }
//            launch {
//                attemptAccept(
//                    thisEndpoint = secondEndpoint,
//                    remotePeerId = firstPeerId,
//                    performHandshake = false,
//                    anotherConnectionJob = connJob
//                )
//            }
        }


        return secondEndpoint
    }

    private suspend fun attemptConnect(
        thisEndpoint: Endpoint,
        remoteEndpoint: Endpoint,
        remotePeerId: PeerId,
        performHandshake: Boolean,
        anotherConnectionJob: Job? = null,
        attemptDelayMillis: Long = 10,
        attemptTimeoutMillis: Long = 1000
    ): Boolean {
        for (attempt in 0 until connectionAttempts) {
            try {
                delay(attemptDelayMillis)

                withTimeout(attemptTimeoutMillis) {
                    transport.rawConnect(
                        remoteEndpoint.toInetSocketAddress(),
                        thisEndpoint.toInetSocketAddress(),
                        performHandshake
                    )
                }
                anotherConnectionJob?.cancel()

                println("Successfully connected to $remotePeerId")
                return true
            } catch (e: IOException) {
                println("$performHandshake, $e to ${remotePeerId.toString().take(5)}")
            }
        }
        println("Failed to connect to $remotePeerId")
        return false
    }

    private suspend fun attemptAccept(
        thisEndpoint: Endpoint,
        remotePeerId: PeerId,
        performHandshake: Boolean,
        anotherConnectionJob: Job? = null,
        attemptTimeoutMillis: Long = 1000
    ): Boolean {
        try {
            println("STARTING ACCEPT ROUTINE")
            withTimeout(attemptTimeoutMillis) {
                transport.rawAccept(
                    thisEndpoint.toInetSocketAddress(),
                    performHandshake
                )
            }
            anotherConnectionJob?.cancel()
            return true
        } catch (e: IOException) {
            println("Failed to accept-connect to $remotePeerId due to:\n $e")
        }
        return false
    }

    private val streamStore = WeakHashMap<PeerId, TCPHolePunchingStream>()

    companion object {
        private val currentVersion = ProtocolVersion(1u, 0u, 0u)
        private val leastSupportedVersion = currentVersion
        val protocolDescriptor =
            ProtocolDescriptor(
                name = "TCPHole",
                currentVersion,
                leastSupportedVersion
            )
    }
}

class TCPHolePunchingStream : StreamLeafNode() {
    override suspend fun receive(message: Buffer) {
        rpcBase.receive(message)
    }

    private val rpcBase = RPCBase { send(it) }

    suspend fun performConnectionFirstStep(
        secondPeerId: PeerId,
        firstEndpoint: Endpoint,
        rpcTimeoutMillis: Long
    ): Endpoint? {
        val request = ConnectionFirstRequest(secondPeerId, firstEndpoint)
        val requestMsg = serializeConnectionFirstRequest(request)

        val responseMsg = rpcBase.makeRPC(
            MessageTypes.CONNECTION_FIRST_STEP.typeId,
            requestMsg,
            rpcTimeoutMillis
        )

        val response = deserializeConnectionFirstResponse(responseMsg)

        return if (response.reachedSecond) response.secondEndpoint else null
    }

    suspend fun performConnectionSecondStep(
        firstPeerId: PeerId,
        firstEndpoint: Endpoint,
        rpcTimeoutMillis: Long
    ): Endpoint {
        val request = ConnectionSecondRequest(firstPeerId, firstEndpoint)
        val requestMsg = serializeConnectionSecondRequest(request)

        val responseMsg = rpcBase.makeRPC(
            MessageTypes.CONNECTION_SECOND_STEP.typeId,
            requestMsg,
            rpcTimeoutMillis
        )
        val response = deserializeConnectionSecondResponse(responseMsg)

        return response.secondEndpoint.apply {
            substituteExternalAddress(this, remoteEndpoint)
        }
    }

    fun onConnectionFirstStepRequest(
        handler:
        suspend (
            firstPeerId: PeerId,
            firstEndpoint: Endpoint,
            secondPeerId: PeerId
        ) -> Endpoint?
    ) {
        rpcBase.rpcHandlers[MessageTypes.CONNECTION_FIRST_STEP.typeId] = { requestMsg ->
            val request = deserializeConnectionFirstRequest(requestMsg)

            val firstEndpoint = substituteExternalAddress(request.firstEndpoint, remoteEndpoint)

            val secondEndpoint = handler(remotePeerId, firstEndpoint, request.secondPeerId)
            // Null endpoint means that we couldn't connect to the second peer
            val response = ConnectionFirstResponse(
                secondEndpoint = secondEndpoint ?: "",
                reachedSecond = secondEndpoint != null
            )
            serializeConnectionFirstResponse(response)
        }
    }

    fun onConnectionSecondStepRequest(
        handler:
        suspend (
            firstPeerId: PeerId,
            firstEndpoint: Endpoint
        ) -> Endpoint
    ) {
        rpcBase.rpcHandlers[MessageTypes.CONNECTION_SECOND_STEP.typeId] = { requestMsg ->
            val request = deserializeConnectionSecondRequest(requestMsg)
            val secondEndpoint = handler(remotePeerId, request.firstEndpoint)

            val response = ConnectionSecondResponse(secondEndpoint)
            serializeConnectionSecondResponse(response)
        }
    }

    private fun substituteExternalAddress(
        givenEndpoint: Endpoint,
        someExternalEndpoint: Endpoint
    ): Endpoint {
        val sockAddr = givenEndpoint.toInetSocketAddress()
        val externalAddr = someExternalEndpoint.toInetSocketAddress().address
        return InetSocketAddress(externalAddr, sockAddr.port).toEndpoint()
    }

    override var opened: Boolean = false

    private fun handleHandshake() {
        opened = true
    }

    private fun handleClosure() {
        opened = false
    }

    init {
        rpcBase.rpcHandlers[MessageTypes.HANDSHAKE.typeId] = { _ ->
            handleHandshake()
            emptyMsg
        }
        rpcBase.rpcHandlers[MessageTypes.CLOSURE.typeId] = { _ ->
            handleClosure()
            emptyMsg
        }
    }

    override suspend fun performHandshake() {
        try {
            rpcBase.makeRPC(
                MessageTypes.HANDSHAKE.typeId,
                emptyMsg,
                handshakeAndClosureTimeout
            )
            opened = true
        } catch (e: TimeoutCancellationException) {
            throw HandshakeFailedException()
        }
    }

    override suspend fun performClosure() {
        try {
            rpcBase.makeRPC(
                MessageTypes.CLOSURE.typeId,
                emptyMsg,
                handshakeAndClosureTimeout
            )
            opened = false
        } catch (e: TimeoutCancellationException) {
            throw ClosureFailedException()
        }
    }

    companion object {
        private const val handshakeAndClosureTimeout: Long = 1000
        private val emptyMsg = ByteArray(size = 0)

        private enum class MessageTypes(val typeId: UByte) {
            CONNECTION_FIRST_STEP(typeId = 0u),
            CONNECTION_SECOND_STEP(typeId = 1u),
            HANDSHAKE(typeId = 2u),
            CLOSURE(typeId = 3u)
        }
    }
}

private class ConnectionFirstRequest(val secondPeerId: PeerId, val firstEndpoint: Endpoint)

private class ConnectionFirstResponse(val secondEndpoint: Endpoint, val reachedSecond: Boolean)

private class ConnectionSecondRequest(val firstPeerId: PeerId, val firstEndpoint: Endpoint)

private class ConnectionSecondResponse(val secondEndpoint: Endpoint)

private fun serializeConnectionFirstRequest(request: ConnectionFirstRequest): ByteArray =
    TCPHolePunchingProtos.ConnectionFirstRequest
        .newBuilder()
        .setFirstEndpoint(request.firstEndpoint)
        .setSecondPeerId(request.secondPeerId.toByteString())
        .build()
        .toByteArray()

private fun serializeConnectionFirstResponse(response: ConnectionFirstResponse): ByteArray =
    TCPHolePunchingProtos.ConnectionFirstResponse
        .newBuilder()
        .setReachedSecond(response.reachedSecond)
        .setSecondEndpoint(response.secondEndpoint)
        .build()
        .toByteArray()

private fun serializeConnectionSecondRequest(request: ConnectionSecondRequest): ByteArray =
    TCPHolePunchingProtos.ConnectionSecondRequest
        .newBuilder()
        .setFirstEndpoint(request.firstEndpoint)
        .setFirstPeerId(request.firstPeerId.toByteString())
        .build()
        .toByteArray()

private fun serializeConnectionSecondResponse(response: ConnectionSecondResponse): ByteArray =
    TCPHolePunchingProtos.ConnectionSecondResponse
        .newBuilder()
        .setSecondEndpoint(response.secondEndpoint)
        .build()
        .toByteArray()

private suspend fun deserializeConnectionFirstRequest(bytes: ByteArray): ConnectionFirstRequest {
    val proto = withContext(Dispatchers.IO) {
        TCPHolePunchingProtos.ConnectionFirstRequest
            .parseFrom(bytes)
    }

    return ConnectionFirstRequest(proto.secondPeerId.toPeerId(), proto.firstEndpoint)
}

private suspend fun deserializeConnectionFirstResponse(bytes: ByteArray): ConnectionFirstResponse {
    val proto = withContext(Dispatchers.IO) {
        TCPHolePunchingProtos.ConnectionFirstResponse
            .parseFrom(bytes)
    }

    return ConnectionFirstResponse(proto.secondEndpoint, proto.reachedSecond)
}

private suspend fun deserializeConnectionSecondRequest(bytes: ByteArray): ConnectionSecondRequest {
    val proto = withContext(Dispatchers.IO) {
        TCPHolePunchingProtos.ConnectionSecondRequest
            .parseFrom(bytes)
    }

    return ConnectionSecondRequest(proto.firstPeerId.toPeerId(), proto.firstEndpoint)
}

private suspend fun deserializeConnectionSecondResponse(bytes: ByteArray): ConnectionSecondResponse {
    val proto = withContext(Dispatchers.IO) {
        TCPHolePunchingProtos.ConnectionSecondResponse
            .parseFrom(bytes)
    }

    return ConnectionSecondResponse(proto.secondEndpoint)
}