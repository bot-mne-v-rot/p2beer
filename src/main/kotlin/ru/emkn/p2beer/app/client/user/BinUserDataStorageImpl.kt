package ru.emkn.p2beer.app.client.user

import ru.emkn.p2beer.app.client.util.temporaryChatDataFile
import java.io.File
import java.io.RandomAccessFile

class TempChatStorage {
    var messagesCount: Long = 0
    var lastMessageTimeStamp: Long = 0
    var publicKey = ByteArray(32)

    fun saveToFile() {
        val file = RandomAccessFile(File(temporaryChatDataFile), "rw")
        file.write(publicKey)
        file.writeLong(messagesCount)
        file.writeLong(lastMessageTimeStamp)
    }

    fun loadFromFile() {
        val file = RandomAccessFile(File(temporaryChatDataFile), "r")
        file.read(publicKey)
        messagesCount = file.readLong()
        lastMessageTimeStamp = file.readLong()
    }
}