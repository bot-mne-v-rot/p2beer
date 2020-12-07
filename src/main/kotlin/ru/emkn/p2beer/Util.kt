package ru.emkn.p2beer
import java.net.Socket
import java.util.*

data class Client(val socket: Socket,
                  val publicAddress: Pair<String, Int>,
                  val privateAddress: Pair<String, Int>)

fun addressToMessage(address: Pair<String, Int>): String = address.first + ":" + address.second.toString()

fun messageToAddress(message: String): Pair<String, Int> {
    val parts = message.split(":")
    return Pair(parts[0], parts[1].toInt())
}

fun sendMessage(socket: Socket, message: String) {
    socket.getOutputStream().write((message + '\n').toByteArray())
    socket.getOutputStream().flush()
}

fun receiveMessage(socket: Socket): String = Scanner(socket.getInputStream()).nextLine()

fun createPeerMessage(client: Client): String = addressToMessage(client.publicAddress) + "|" + addressToMessage(client.privateAddress)