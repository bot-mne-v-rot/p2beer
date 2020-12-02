package ru.emkn.p2beer
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import java.util.logging.Logger

fun main() {
    val host = "35.217.31.232"
    val port = 5005
    val sa = Socket(host, port)
    sa.reuseAddress = true

    val privAddr = InetSocketAddress(sa.inetAddress, port)

    //println(sendMsg(sa, addrToMsg(privAddr)))
    //val logger = Logger.getLogger("ab")
    //logger.info("")
}