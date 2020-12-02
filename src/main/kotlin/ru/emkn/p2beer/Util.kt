package ru.emkn.p2beer
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.ServerSocket
import java.net.SocketAddress
import java.nio.ByteBuffer

fun addrToMsg(addr: Pair<String, Int>) = addr.first + ":" + addr.second.toString()

fun msgToAddr(msg: String): Pair<String, Int> {
    val parts = msg.split(":")
    return Pair(parts[0], parts[1].toInt())
}

fun sendMsg(sock: Socket, msg: String, writer: OutputStream) {
    println(msg)
    val ar = ByteBuffer.allocate(4).putInt(msg.length).array() + msg.toByteArray()
    writer.write(ar)
    //println(ar.joinToString(" "))

    //sock.getOutputStream().write(ar)
}

fun receiveMsg(sock: Socket): String {
    val x = sock.getInputStream().readBytes()
    val z = x.copyOfRange(4, x.size)
    return z.decodeToString()
}