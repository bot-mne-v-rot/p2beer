package p2p.network

import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import io.mockk.*
import kotlin.test.*

import kotlinx.coroutines.*

import org.reflections.*
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.*
import java.lang.reflect.*

import ru.emkn.p2beer.p2p.network.*

class StreamPeerCommonTests {
    @ParameterizedTest
    @MethodSource("streamNodeFactoriesToTestHandshakeAndClosure")
    fun `test handshake and closure with StreamNode`(streamNodeFactory: () -> StreamNode) = runBlocking {
        val connector = StreamConnector()

        val first = streamNodeFactory()
        val second = streamNodeFactory()

        connector.left = streamNodeFactory()
        connector.right = streamNodeFactory()

        first.performHandshake()

        assertTrue(first.opened)
        assertTrue(second.opened)

        first.performClosure()

        assertFalse(first.opened)

        // We don't check the second for being closed because
        // the streams might not exchange the messages needed
        // to close each other
        //// assertFalse(second.opened)
    }

    /**
     * Actually, code duplication goes on here but I have no clue
     * how to beautifully extract the common part here
     */
    @ParameterizedTest
    @MethodSource("streamListNodeFactoriesToTestHandshakeAndClosure")
    fun `test handshake and closure with StreamListNode`(streamListNodeFactory: () -> StreamListNode) = runBlocking {

        val connector = StreamConnector()

        val first = streamListNodeFactory()
        val second = streamListNodeFactory()

        connector.left = first
        connector.right = second

        val streamMock = mockk<StreamLeafNode>()

        coEvery { streamMock.performHandshake() } returns Unit
        coEvery { streamMock.performClosure() } returns Unit
        every { streamMock.parent = any() } returns Unit

        first.child = streamMock
        first.performHandshake()

        assertTrue(first.opened)
        assertTrue(second.opened)
        coVerify { streamMock.performHandshake() }

        second.performClosure()

        assertFalse(second.opened)
    }

    @ParameterizedTest
    @MethodSource("streamListNodeFactoriesToTestSendAndReceive")
    fun `test send and receive with StreamListNode`(streamListNodeFactory: () -> StreamListNode) = runBlocking {
        val connector = StreamConnector()

        val first = streamListNodeFactory()
        val second = streamListNodeFactory()

        connector.left = first
        connector.right = second

        val initMock: (StreamLeafNode) -> Unit =
            { mock: StreamLeafNode ->
                every { mock.parent = any() } returns Unit
                coEvery { mock.performHandshake() } returns Unit
                coEvery { mock.performClosure() } returns Unit
                coEvery { mock.receive(any()) } returns Unit
            }

        val streamMock1 = mockk<StreamLeafNode>()
        initMock(streamMock1)

        val streamMock2 = mockk<StreamLeafNode>()
        initMock(streamMock2)

        first.child = streamMock1
        second.child = streamMock2

        first.performHandshake()

        val msgA = "A".toByteArray()
        val msgB = "B".toByteArray()

        first.send(msgA)
        coVerify {
            streamMock2.receive(msgA)
        }

        second.send(msgB)
        coVerify {
            streamMock1.receive(msgB)
        }
    }

    companion object {
        private inline fun
                <reified I, reified AT : Annotation, reified AR : Annotation>
                templatedArguments(): Set<Arguments> {
            val reflections = Reflections(
                ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage("p2p"))
                    .setScanners(MethodAnnotationsScanner())
            )
            val annotated: Set<Method> =
                reflections.getMethodsAnnotatedWith(AT::class.java)
                    .intersect(reflections.getMethodsAnnotatedWith(AR::class.java))


            return annotated
                .filter { method -> Modifier.isStatic(method.modifiers) }
                .map { method ->
                    Arguments.of({
                        method.invoke(null) as I
                    })
                }
                .toSet()
        }

        private val lazyStreamNodeFactoriesToTestHandshakeAndClosure by lazy {
            templatedArguments<StreamNode, TestHandshakeAndClosure, StreamNodeFactory>()
        }

        private val lazyStreamListNodeFactoriesToTestHandshakeAndClosure by lazy {
            templatedArguments<StreamListNode, TestHandshakeAndClosure, StreamListNodeFactory>()
        }

        private val lazyStreamListNodeFactoriesToTestSendAndReceive by lazy {
            templatedArguments<StreamListNode, TestSendAndReceive, StreamListNodeFactory>()
        }

        @JvmStatic
        fun streamNodeFactoriesToTestHandshakeAndClosure(): Set<Arguments> {
            return lazyStreamNodeFactoriesToTestHandshakeAndClosure
        }

        @JvmStatic
        fun streamListNodeFactoriesToTestHandshakeAndClosure(): Set<Arguments> {
            return lazyStreamListNodeFactoriesToTestHandshakeAndClosure
        }

        @JvmStatic
        fun streamListNodeFactoriesToTestSendAndReceive(): Set<Arguments> {
            return lazyStreamListNodeFactoriesToTestSendAndReceive
        }
    }
}

/**
 * Annotate your static factory with this annotation with
 * @JVMStatic and some of the factories annotations
 * and have your [StreamNode] automatically
 * tested with some common tests.
 *
 * Supports [StreamNodeFactory] and [StreamListNodeFactory]
 */
@Target(AnnotationTarget.FUNCTION)
annotation class TestHandshakeAndClosure

/**
 * Annotate your static factory with this annotation with
 * @JVMStatic and some of the factories annotations
 * and have your [StreamNode] automatically
 * tested with some common tests.
 *
 * Supports [StreamListNodeFactory] only
 */
@Target(AnnotationTarget.FUNCTION)
annotation class TestSendAndReceive

@Target(AnnotationTarget.FUNCTION)
annotation class StreamNodeFactory

@Target(AnnotationTarget.FUNCTION)
annotation class StreamListNodeFactory


