package ru.emkn.p2beer.app.client.chat.bTree

import ru.emkn.p2beer.app.client.chat.*
import java.io.File
import java.io.RandomAccessFile

class BTree(private val maxNumberOfChildren: Int = 10,
            private val pathToIndexFile: String,
            private val pathToMessagesFile: String) {

    private val indexFile = RandomAccessFile(File(pathToIndexFile), "rw")
    private val messagesFile = RandomAccessFile(File(pathToMessagesFile), "rw")

    var root = Node(0, mutableListOf(), mutableListOf(),
        maxNumberOfChildren, indexFile, messagesFile)

    init {
        if (indexFile.length() == 0.toLong()) {
            messagesFile.writeInt(0)
            indexFile.writeNode(root)
        }
        else
            root = indexFile.readNode(0, maxNumberOfChildren, indexFile, messagesFile)
    }

    fun addMessage(message: Message) {
        val pointerToMessage = messagesFile.writeMessageToTheEnd(message)
        increaseNumberOfMessages()
        if (root.isFull)
            root = createNewRoot()
        root.insertMessageInNonFull(message, pointerToMessage)
    }

    private fun increaseNumberOfMessages() {
        val currentNumberOfMessages = getCurrentNumberOfMessages()
        messagesFile.seek(0)
        messagesFile.writeInt(currentNumberOfMessages + 1)
    }

    private fun createNewRoot(): Node {
        val newRoot = Node(indexFile.length(), mutableListOf(), mutableListOf(0), maxNumberOfChildren, indexFile, messagesFile)
        indexFile.writeNode(newRoot)
        newRoot.splitChild(root, 0)
        newRoot.pointersToChildren[0] = newRoot.positionInIndexFile
        newRoot.swapWith(root)
        return newRoot
    }

    fun getKNextMessages(message: Message, k: Int): List<Message> =
        root.findKNextMessages(message, k)

    fun getKPreviousMessages(message: Message, k: Int): List<Message> =
        root.findKPreviousMessages(message, k).reversed()

    fun getLastMessage(): Message? =
        root.findLastMessage()

    fun getKLastMessages(k: Int): List<Message> {
        val lastMessage = getLastMessage() ?: return listOf()
        return getKPreviousMessages(lastMessage, k - 1) + listOf(lastMessage)
    }

    fun getCurrentNumberOfMessages(): Int {
        messagesFile.seek(0)
        return messagesFile.readInt()
    }
}
