package p2p.network

import org.junit.jupiter.api.*
import io.mockk.*

import kotlinx.coroutines.*

import ru.emkn.p2beer.p2p.network.StreamToChannelAdapter
import ru.emkn.p2beer.p2p.network.StreamListNode
import kotlin.test.assertEquals

class StreamToChannelAdapterTest {
    @Test
    fun `test stream write`() = runBlocking {
        val streamMock = mockk<StreamListNode>()
        val adapter = StreamToChannelAdapter()

        coEvery { streamMock.send(any()) } returns Unit
        adapter.parent = streamMock

        val job = launch(CoroutineName("Adapter")) {
            adapter.run()
        }

        val message = "A".toByteArray()

        adapter.sendChannel.send(message)
        delay(100)

        coVerify { streamMock.send(message) }
        job.cancel()
    }

    @Test
    fun `test stream read`() = runBlocking {
        val adapter = StreamToChannelAdapter()
        val message = "A".toByteArray()

        adapter.receive(message)
        assertEquals(adapter.receiveChannel.receive(), message)
    }
}