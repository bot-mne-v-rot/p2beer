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
            import kotlin.system.exitProcess


    fun main() {
        val host = "35.217.31.232"
        val port = 5005
        val sa = Socket(host, port)
        sa.reuseAddress = true

        val privAddr = Pair(sa.localAddress.hostAddress, sa.localPort)
        println("${privAddr.first}:${privAddr.second}")
        val writer = sa.getOutputStream()

    sendMsg(sa, addrToMsg(privAddr), writer)

    var data = receiveMsg(sa)

    val logger = Logger.getLogger("client")

    logger.info("client ${privAddr.first}:${privAddr.second} - received data: ${data}")
    val pubAddr = msgToAddr(data)
    //println("${pubAddr.first}:${pubAddr.second}")

    sendMsg(sa, addrToMsg(pubAddr), writer)

    data = receiveMsg(sa)
    val clientPubAddr = msgToAddr(data.split("|")[0])
    val clientPrivAddr = msgToAddr(data.split("|")[1])
    logger.info(
        "client public is ${pubAddr.first}:${pubAddr.second} and private is ${privAddr.first}:${privAddr.second}, " +
                "peer public is ${clientPubAddr.first}:${clientPubAddr.second} private is ${clientPrivAddr.first}:${clientPrivAddr.second}"
    )
}