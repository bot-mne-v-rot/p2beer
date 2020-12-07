package p2p.network

import ru.emkn.p2beer.p2p.Buffer

import java.lang.IllegalStateException
import kotlin.coroutines.*
import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.network.*

private class ExampleStreamListNode : StreamListNode() {
    override var opened: Boolean = false

    private val handshakeMsg = "HaNdShAkE123".toByteArray()
    private val handshakeRespMsg = "HaNdShAkE123Resp".toByteArray()
    private val closureMsg = "ClOsUrE123".toByteArray()
    private val closureRespMsg = "ClOsUrE123Resp".toByteArray()

    private var handshakeContinuation: Continuation<Unit>? = null
    private var closureContinuation: Continuation<Unit>? = null

    override suspend fun receive(message: Buffer) {
        val isHandshake = message.contentEquals(handshakeMsg)
        val isHandshakeResp = message.contentEquals(handshakeRespMsg)
        val isClosure = message.contentEquals(closureMsg)
        val isClosureResp = message.contentEquals(closureRespMsg)

        if (!opened && isHandshake) {
            opened = true
            send(handshakeRespMsg)
        } else if (opened && isClosure) {
            send(closureRespMsg)
            opened = false
        } else if (!opened && isHandshakeResp && handshakeContinuation != null) {
            val cont = handshakeContinuation
            handshakeContinuation = null
            cont?.resume(Unit)
        } else if (opened && isClosureResp && closureContinuation != null) {
            val cont = closureContinuation
            closureContinuation = null
            cont?.resume(Unit)
        } else if (!opened || isHandshake || isClosure || isHandshakeResp || isClosureResp) {
            handshakeContinuation?.resumeWithException(HandshakeFailedException())
            closureContinuation?.resumeWithException(ClosureFailedException())
            throw IllegalStateException()
        } else
            child?.receive(message)
    }

    override suspend fun send(message: Buffer) {
        if (!opened)
            throw IllegalStateException()
        else
            parent?.send(message)
    }

    override suspend fun performHandshake(): Unit = coroutineScope {
        if (opened)
            throw IllegalStateException()
        else {
            suspendCoroutine<Unit> {
                handshakeContinuation = it
                launch { parent?.send(handshakeMsg) }
            }
            opened = true

            child?.performHandshake()
        }
    }

    override suspend fun performClosure(): Unit = coroutineScope {
        if (!opened)
            throw IllegalStateException()
        else {
            child?.performClosure()

            suspendCoroutine<Unit> {
                closureContinuation = it
                launch { parent?.send(closureMsg) }
            }

            opened = false
        }
    }
}

class ExampleStreamListNodeTests() {
    companion object {
        @StreamListNodeFactory
        @TestHandshakeAndClosure
        @TestSendAndReceive
        @JvmStatic
        fun factoryForExample() : StreamListNode =
            ExampleStreamListNode()
    }
}