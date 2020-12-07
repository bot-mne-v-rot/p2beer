package ru.emkn.p2beer.p2p.network

import ru.emkn.p2beer.p2p.Buffer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

class StreamToChannelAdapter(bufferSize: Int = 10) : StreamLeafNode() {

    // Names are chosen as they are supposed to look for the end-user
    val sendChannel: SendChannel<Buffer>
        get() = inChannel
    val receiveChannel: ReceiveChannel<Buffer>
        get() = outChannel

    private val inChannel = Channel<Buffer>(bufferSize)
    private val outChannel = Channel<Buffer>(bufferSize)

    suspend fun run() {
        sendChannelData()
    }

    private suspend fun sendChannelData(): Unit = coroutineScope {
        for (message in inChannel)
            launch { send(message) }
    }

    override suspend fun receive(message: Buffer) {
        outChannel.send(message)
    }
}