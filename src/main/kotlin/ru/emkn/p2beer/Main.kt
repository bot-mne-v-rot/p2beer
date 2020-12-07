package ru.emkn.p2beer
import java.net.Socket
import java.util.logging.Logger
import kotlinx.coroutines.*

fun main() = runBlocking {
    val host = "35.217.31.232"
    val port = 5005
    val socket = Socket(host, port)
    socket.reuseAddress = true
    val logger = Logger.getLogger("client")

    val privateAddress = Pair(socket.localAddress.hostAddress, socket.localPort)

    sendMessage(socket, addressToMessage(privateAddress))

    var receivedData = receiveMessage(socket)
    logger.info("client ${addressToMessage(privateAddress)} - received data: $receivedData")
    val publicAddress = messageToAddress(receivedData)

    sendMessage(socket, addressToMessage(publicAddress))

    receivedData = receiveMessage(socket)
    val clientPublicAddress = messageToAddress(receivedData.split("|")[0])
    val clientPrivateAddress = messageToAddress(receivedData.split("|")[1])
    logger.info(
        "client public is ${publicAddress.first}:${publicAddress.second} and private is ${privateAddress.first}:${privateAddress.second}, " +
                "peer public is ${clientPublicAddress.first}:${clientPublicAddress.second} private is ${clientPrivateAddress.first}:${clientPrivateAddress.second}"
    )

    // try to connect from our privateAddress to clientPublicAddress...

}