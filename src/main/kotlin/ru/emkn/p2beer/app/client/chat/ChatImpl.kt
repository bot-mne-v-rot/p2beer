package ru.emkn.p2beer.app.client.chat

import ru.emkn.p2beer.app.client.util.defaultLastSeen
import kotlin.collections.ArrayDeque

//TODO: Add pubkey to ChatImpl constructor
class ChatImpl (private val userName : String) : Chat {

    private val loadedMessages : ArrayDeque<Message> = ArrayDeque()

    var onlineStatus : Boolean = false

    val messagesCount = 0

    /**
     * 13 September 2009 г., 14:22:08
     * Btw 13 September is a programmer's day
     */

    var lastSeen : Long = defaultLastSeen

    override fun addMessage(message: Message) {
        this.loadedMessages.addFirst(message)
    }

    override fun updateOnlineStatus(newStatus: Boolean) {
        this.onlineStatus = newStatus
    }

    override fun toString () = this.userName
}