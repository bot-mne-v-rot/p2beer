package p2p.network

import org.junit.jupiter.api.*
import kotlin.test.*

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.network.*

import ru.emkn.p2beer.p2p.Buffer

class ProtocolRouterTests {
    companion object {
        @StreamListNodeFactory
        @TestHandshakeAndClosure
        @TestSendAndReceive
        @JvmStatic
        fun protocolRouterFactory() : StreamListNode {
            val stream = StreamListNode()
            val router = ProtocolRouterStreamImpl(setOf(
                ProtocolDescriptor("a", ProtocolVersion(1u, 0u, 0u))
            ))
            router.protocols.values.first().child = stream

            // Making both streams appear together
            return object : StreamListNode() {
                override var parent: StreamNode?
                    get() = router.parent
                    set(value) { router.parent = value }

                override var child: StreamNode?
                    get() = stream.child
                    set(value) { stream.child = value }

                override suspend fun performHandshake() {
                    router.performHandshake()
                    opened = router.opened
                }

                override suspend fun performClosure() {
                    router.performClosure()
                    opened = router.opened
                }

                override suspend fun receive(message: Buffer) {
                    router.receive(message)
                }

                override suspend fun send(message: Buffer) {
                    stream.send(message)
                }
            }
        }
    }
}