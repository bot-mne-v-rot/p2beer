package ru.emkn.p2beer.app.client.chat

import ru.emkn.p2beer.app.client.user.Friend
import kotlin.collections.ArrayDeque

//TODO: Add pubkey to ChatImpl constructor
class ChatImpl (val friend : Friend) : Chat {

    private val loadedMessages : ArrayDeque<Message> = ArrayDeque()
    lateinit var firstLoadedMessage : Message
    var loadedMessagesCount = 0

    /**
     * 13 September 2009 г., 14:22:08
     * 13 September is a programmer's day
     */

    override fun addMessage(message: Message) {
        this.loadedMessages.addFirst(message)
    }

    override fun toString () = this.friend.userInfo.userName
}