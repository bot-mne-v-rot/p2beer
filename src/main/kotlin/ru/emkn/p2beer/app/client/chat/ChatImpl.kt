package ru.emkn.p2beer.app.client.chat

import ru.emkn.p2beer.app.p2bLib.*
import kotlin.collections.ArrayDeque

class ChatImpl (userId : PublicKeyRSA, userName : String ) : Chat {

    private val loadedMessages : ArrayDeque<Message> = ArrayDeque()

    var onlineStatus : Boolean = false

    /**
     * 13 September 2009 г., 14:22:08
     * Btw 13 September is a programmer's day
     */

    var lastSeen : Long = 1252851728

    override fun addMessage(message: Message) {
        this.loadedMessages.addFirst(message)
    }

    override fun updateOnlineStatus(newStatus: Boolean) {
        this.onlineStatus = newStatus
    }
}