package ru.emkn.p2beer
import java.net.*
import java.util.logging.Logger

fun main() {
    val logger = Logger.getLogger("")

    val port = 5005

    val serverSocket = ServerSocket(port)
    serverSocket.reuseAddress = true
    serverSocket.soTimeout = 30

    var client1: Client? = null

    while (true) {
        val socket: Socket
        val addressOfConnection: Pair <String, Int>
        try {
            socket = serverSocket.accept()
            addressOfConnection = Pair(socket.inetAddress.hostAddress, socket.port)
        } catch (e: SocketTimeoutException) {
            continue
        }
        logger.info("new connection: ${addressToMessage(addressOfConnection)}")

        var receivedData = receiveMessage(socket)
        val privateAddress = messageToAddress(receivedData)
        logger.info("receive privateAddress: ${addressToMessage(privateAddress)}")

        sendMessage(socket, addressToMessage(addressOfConnection))
        receivedData = receiveMessage(socket)
        val receivedAddress = messageToAddress(receivedData)

        if (receivedAddress == addressOfConnection) {
            logger.info("client reply matches")
            val client2 = Client(socket, addressOfConnection, privateAddress)
            if (client1 == null)
                client1 = client2
            else if(client1 != client2) {
                sendMessage(client1.socket, createPeerMessage(client2))
                logger.info("server - send client info to ${client1.publicAddress}")

                sendMessage(client2.socket, createPeerMessage(client1))
                logger.info("server - send client info to ${client2.publicAddress}")

                client1 = null
            }
        }
        else {
            logger.info("client reply did not match")
            socket.close()
        }
    }
}
