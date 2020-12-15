package ru.emkn.p2beer.app.client.chat

class ChatList {
    var chats : Set<Chat> = setOf()

    val storage : ChatStorage = JSONChatStorageImpl()

    var amountOfMessagesISent : Int = chats.size
}