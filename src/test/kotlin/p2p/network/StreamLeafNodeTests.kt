package p2p.network

import org.junit.jupiter.api.*
import kotlin.test.*

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.network.*

class StreamLeafNodeTests {
    companion object {
        @StreamNodeFactory
        @TestHandshakeAndClosure
        @TestSendAndReceive
        @JvmStatic
        fun protocolRouterFactory() : StreamNode =
            StreamLeafNode()
    }
}