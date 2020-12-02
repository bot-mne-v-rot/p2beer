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
    //val host: String = "0.0.0.0"
    val port = 5005

    val s = ServerSocket(port)
    s.reuseAddress = true
    //s.bind(s.localSocketAddress)
    s.soTimeout = 30

    val sa = s.accept()

    val logger = Logger.getLogger("")
    logger.info("connection address : ${sa.inetAddress}")


}
