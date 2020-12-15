package ru.emkn.p2beer.app.client.chat

import ru.emkn.p2beer.app.client.user.Friend
import kotlin.collections.ArrayDeque

//TODO: Add pubkey to ChatImpl constructor
class ChatImpl (val friend : Friend) : Chat {

    private val loadedMessages : ArrayDeque<Message> = ArrayDeque()

    /**
     * 13 September 2009 г., 14:22:08
     * 13 September is a programmer's day
     */

    override fun addMessage(message: Message) {
        this.loadedMessages.addFirst(message)
    }

    override fun toString () = this.friend.userInfo.userName


//    val messagesCount = friend.messagesCount
//    var onlineStatus : Boolean = this.friend.userInfo.onlineStatus
//    var lastSeen : Long = defaultLastSeen

//    override fun updateOnlineStatus(newStatus: Boolean) {
//        this.onlineStatus = newStatus
//    }
//
//    override fun updateLastSeen(newLastSeen: Long) {
//        this.lastSeen = newLastSeen
//    }

}