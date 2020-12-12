package ru.emkn.p2beer.app.client.chat

interface ChatStorage {
    fun loadChatList()

    fun saveMessage()
}