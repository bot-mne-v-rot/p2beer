package ru.emkn.p2beer.p2p.network.transports.messaging

import java.net.InetSocketAddress
import java.nio.ByteBuffer

interface Socket {
    suspend fun bind(localAddress: InetSocketAddress)
    suspend fun connect(address: InetSocketAddress)
    suspend fun close()
    suspend fun read(buffer: ByteBuffer): Int
    suspend fun write(buffer: ByteBuffer): Int
}