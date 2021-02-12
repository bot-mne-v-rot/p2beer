package ru.emkn.p2beer.app.client.chat.bTree

import ru.emkn.p2beer.app.client.chat.*
import java.io.RandomAccessFile

class Node(var positionInIndexFile: Long,
           val pointersToMessages: MutableList<Long>,
           val pointersToChildren: MutableList<Long>,
           val maxNumberOfChildren: Int,
           val indexFile: RandomAccessFile,
           val messagesFile: RandomAccessFile) {

    val isLeaf: Boolean
        get() = pointersToChildren.size == 0

    val isFull: Boolean
        get() = pointersToMessages.size == maxNumberOfChildren - 1

    fun insertMessageInNonFull(message: Message, pointerToMessage: Long) {
        val messages = getMessages()
        val positionToInsert = messages.upperBound(message)
        if (isLeaf) {
            pointersToMessages.add(positionToInsert, pointerToMessage)
            indexFile.writeNode(this)
            return
        }
        val appropriateChild = indexFile.readNode(pointersToChildren[positionToInsert], maxNumberOfChildren,
            indexFile, messagesFile)
        if (appropriateChild.isFull) {
            splitChild(appropriateChild, positionToInsert)
            insertMessageInNonFull(message, pointerToMessage)
        }
        else
            appropriateChild.insertMessageInNonFull(message, pointerToMessage)
    }

    fun splitChild(child: Node, id: Int) {
        val middle = maxNumberOfChildren / 2
        val halfOfChild = Node(indexFile.length(),
            child.pointersToMessages.subList(middle, maxNumberOfChildren - 1),
            if (child.isLeaf) mutableListOf() else child.pointersToChildren.subList(middle, maxNumberOfChildren),
            maxNumberOfChildren, indexFile, messagesFile)

        indexFile.writeNode(halfOfChild)

        pointersToMessages.add(id, child.pointersToMessages[middle - 1])
        pointersToChildren.add(id + 1, halfOfChild.positionInIndexFile)
        indexFile.writeNode(this)

        child.pointersToMessages.subList(middle - 1, maxNumberOfChildren - 1).clear()
        if (!child.isLeaf)
            child.pointersToChildren.subList(middle, maxNumberOfChildren).clear()
        indexFile.writeNode(child)
    }

    fun findKNextMessages(message: Message, k: Int): List<Message> {
        if (k <= 0)
            return mutableListOf()
        val messagesInThisNode = getMessages()
        val position = messagesInThisNode.upperBound(message)

        val kNextMessages = mutableListOf<Message>()

        for (i in position until messagesInThisNode.size) {
            if (!isLeaf) {
                val child = indexFile.readNode(pointersToChildren[i], maxNumberOfChildren, indexFile, messagesFile)
                kNextMessages += child.findKNextMessages(message, k - kNextMessages.size)
            }
            if (kNextMessages.size < k) kNextMessages.add(messagesInThisNode[i]) else break
        }

        if (!isLeaf && kNextMessages.size < k) {
            val lastChild = indexFile.readNode(pointersToChildren.last(), maxNumberOfChildren, indexFile, messagesFile)
            kNextMessages += lastChild.findKNextMessages(message, k - kNextMessages.size)
        }
        return kNextMessages
    }

    private fun List<Message>.upperBound(message: Message): Int {
        val position = this.binarySearch(message, MessageComparator)
        return if (position >= 0) position + 1 else -position - 1
    }

    fun findKPreviousMessages(message: Message, k: Int): List<Message> {
        if (k <= 0)
            return listOf()
        val messagesInThisNode = getMessages()
        val position = messagesInThisNode.lowerBound(message) - 1

        val kPreviousMessages = mutableListOf<Message>()

        if (!isLeaf) {
            val child = indexFile.readNode(pointersToChildren[position + 1], maxNumberOfChildren, indexFile, messagesFile)
            kPreviousMessages += child.findKPreviousMessages(message, k)
        }

        for (i in position downTo 0) {
            if (kPreviousMessages.size < k) kPreviousMessages.add(messagesInThisNode[i]) else break
            if (!isLeaf) {
                val child = indexFile.readNode(pointersToChildren[i], maxNumberOfChildren, indexFile, messagesFile)
                kPreviousMessages += child.findKPreviousMessages(message, k - kPreviousMessages.size)
            }
        }
        return kPreviousMessages
    }

    private fun List<Message>.lowerBound(message: Message): Int {
        val position = this.binarySearch(message, MessageComparator)
        return if (position >= 0) position else -position - 1
    }

    fun findLastMessage(): Message? {
        if (isLeaf) {
            if (pointersToMessages.size == 0)
                return null
            return messagesFile.readMessage(pointersToMessages.last())
        }
        val lastChild = indexFile.readNode(pointersToChildren.last(), maxNumberOfChildren, indexFile, messagesFile)
        return lastChild.findLastMessage()
    }

    fun swapWith(node: Node) {
        positionInIndexFile = node.positionInIndexFile
            .also { node.positionInIndexFile = positionInIndexFile }
        indexFile.writeNode(this)
        indexFile.writeNode(node)
    }

    fun getMessages(): List<Message> {
        val messages = mutableListOf<Message>()
        for (pointer in pointersToMessages)
            messages.add(messagesFile.readMessage(pointer))
        return messages
    }
}
