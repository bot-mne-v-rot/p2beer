package ru.emkn.p2beer.p2p.network

import kotlin.coroutines.resume
import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.*

import com.google.protobuf.ByteString
import java.nio.ByteBuffer

/**
 * Receives request and returns response
 * Should not throw exceptions
 */
typealias RPCHandler = suspend (ByteArray) -> ByteArray

/**
 * Does dirty work of keeping
 * suspended coroutines while
 * waiting for response.
 *
 * Is has very simple and straightforward implementation
 * ans is not sustainable to any attacks.
 */
class RPCBase(val send: suspend (Buffer) -> Unit) {
    suspend fun receive(message: Buffer) {
        val buffer = ByteBuffer.wrap(message)

        val signature = extractRPCSignature(buffer)
        val type = extractRPCType(buffer)
        val id = extractRPCId(buffer)
        val body = extractBody(buffer)

        when (signature) {
            requestSignature -> handleRPCRequest(type, id, body)
            responseSignature -> handleRPCResponse(id, body)
            else -> return
        }
    }

    val rpcHandlers = mutableMapOf<UByte, RPCHandler>()

    /**
     * Main method. Performs RPC request of specified [type]
     * and awaits for a response.
     *
     * Handler of the [type] on the other side receives the
     * [message] and constructs corresponding response.
     *
     * @throws TimeoutCancellationException if timeout of [timeoutMillis] occurs
     */
    suspend fun makeRPC(type: UByte, message: ByteArray, timeoutMillis: Long = 5000): ByteArray {
        val id = rpcCounter++
        val request = createMessage(requestSignature, type, id, message)

        return withTimeout(timeoutMillis) {
            sendRequestAndAwaitResponse(id, request)
        }
    }

    private suspend fun sendRequestAndAwaitResponse(
        id: UInt,
        request: ByteArray
    ): ByteArray = coroutineScope {
        val sending = launch(start = CoroutineStart.LAZY) {
            send(request)
        }
        suspendCancellableCoroutine {
            pendingRPCs[id] = it
            sending.start() // sending only after we setup a response handler
        }
    }

    private fun createMessage(signature: Byte, type: UByte, id: UInt, body: ByteArray): ByteArray {
        val messageSize = Byte.SIZE_BYTES + UByte.SIZE_BYTES + UInt.SIZE_BYTES + body.size
        val result = ByteArray(messageSize)
        val buffer = ByteBuffer.allocate(messageSize)

        putRPCSignature(signature, buffer)
        putRPCType(type, buffer)
        putRPCId(id, buffer)
        putRPCBody(body, buffer)

        buffer.array().copyInto(result)
        return result
    }

    private suspend fun handleRPCRequest(type: UByte, id: UInt, requestBody: ByteArray) {
        val responseBody = rpcHandlers[type]!!.invoke(requestBody)
        val response = createMessage(responseSignature, type, id, responseBody)
        send(response)
    }

    private fun handleRPCResponse(id: UInt, responseBody: ByteArray) {
        pendingRPCs[id]?.resume(responseBody)
    }

    private fun putRPCSignature(signature: Byte, buffer: ByteBuffer) {
        buffer.put(signature)
    }

    private fun extractRPCSignature(buffer: ByteBuffer): Byte =
        buffer.get()

    private fun putRPCType(type: UByte, buffer: ByteBuffer) {
        buffer.put(type.toByte())
    }

    private fun extractRPCType(buffer: ByteBuffer): UByte =
        buffer.get().toUByte()

    private fun putRPCId(id: UInt, buffer: ByteBuffer) {
        buffer.putInt(id.toInt())
    }

    private fun extractRPCId(buffer: ByteBuffer): UInt =
        buffer.int.toUInt()

    private fun putRPCBody(body: ByteArray, buffer: ByteBuffer) {
        buffer.put(body)
    }

    private fun extractBody(buffer: ByteBuffer): ByteArray {
        val result = ByteArray(buffer.remaining())
        buffer.array().copyInto(result, startIndex = buffer.position())
        return result
    }

    companion object {
        private const val requestSignature: Byte = 0
        private const val responseSignature: Byte = 1
    }

    private val pendingRPCs = mutableMapOf<UInt, CancellableContinuation<ByteArray>>()

    private var rpcCounter = 0u
}

fun PeerId.toByteString(): ByteString =
    ByteString.copyFrom(data.toByteArray())

// We pay a huge cost to work with experimental unsigned types
fun ByteString.toPeerId() =
    PeerId(toByteArray().toUByteArray())