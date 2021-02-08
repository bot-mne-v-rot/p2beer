package ru.emkn.p2beer.app.client.chat

import java.io.File
import java.io.RandomAccessFile
import javax.swing.text.Position
import kotlin.math.max
import kotlin.properties.Delegates

class BTree(val maxNumberOfChildren: Int = 10,
            private val pathToIndexFile: String,
            private val pathToMessagesFile: String) {

    val indexFile = RandomAccessFile(File(pathToIndexFile), "rw")
    val messagesFile = RandomAccessFile(File(pathToMessagesFile), "rw")

    var root = Node(0, mutableListOf(), mutableListOf(), maxNumberOfChildren)

    init {
        if (indexFile.length() == 0.toLong()) {
            messagesFile.writeInt(0)
            indexFile.writeNode(root)
        }
        else
            root = indexFile.readNode(0, maxNumberOfChildren)
    }

    fun addMessage(message: Message) {
        val pointerToMessage = messagesFile.writeMessageToTheEnd(message)
        increaseNumberOfMessages()
        if (root.isFull()) {
            val newRoot = Node(indexFile.length(), mutableListOf(), mutableListOf(0), maxNumberOfChildren)
            indexFile.writeNode(newRoot)
            splitChild(newRoot, root, 0)

            val firstChild = indexFile.readNode(newRoot.pointersToChildren[0], maxNumberOfChildren)
            val secondChild = indexFile.readNode(newRoot.pointersToChildren[1], maxNumberOfChildren)
            //println(firstChild.positionInIndexFile)
            //println(secondChild.positionInIndexFile)
            //println("${firstChild.pointersToMessages.size} ${firstChild.pointersToChildren.size}")
            //println("${secondChild.pointersToMessages.size} ${secondChild.pointersToChildren.size}")

            newRoot.pointersToChildren[0] = newRoot.positionInIndexFile
            swapNodes(newRoot, root)
            root = newRoot
        }
        /*if (root.pointersToChildren.size > 0) {
            val firstChild = indexFile.readNode(root.pointersToChildren[0], maxNumberOfChildren)
            val secondChild = indexFile.readNode(root.pointersToChildren[1], maxNumberOfChildren)
            println("${firstChild.pointersToMessages.size} ${firstChild.pointersToChildren.size}")
            println("${secondChild.pointersToMessages.size} ${secondChild.pointersToChildren.size}")
        }*/
        //println("${root.pointersToMessages.size} ${root.pointersToChildren.size}")
        insertMessageInNonFull(root, message, pointerToMessage)

    }

    private fun insertMessageInNonFull(node: Node, message: Message, pointerToMessage: Long) {
        val messages = node.getMessages(messagesFile)
        val positionToInsert = findPositionOfFirstGreater(messages, message)
        if (node.isLeaf()) {
            node.pointersToMessages.add(positionToInsert, pointerToMessage)
            indexFile.writeNode(node)
            return
        }
        val appropriateChild = indexFile.readNode(node.pointersToChildren[positionToInsert], maxNumberOfChildren)
        //println("${node.pointersToChildren.size} ${appropriateChild.pointersToChildren.size}")
        if (appropriateChild.isFull()) {
            splitChild(node, appropriateChild, positionToInsert)
            insertMessageInNonFull(node, message, pointerToMessage)
        }
        else
            insertMessageInNonFull(appropriateChild, message, pointerToMessage)
    }

    private fun splitChild(node: Node, child: Node, id: Int) {
        val middle = maxNumberOfChildren / 2
        val halfOfChild = Node(
            indexFile.length(),
            child.pointersToMessages.subList(middle, maxNumberOfChildren - 1),
            if (child.isLeaf()) mutableListOf() else child.pointersToChildren.subList(middle, maxNumberOfChildren),
            maxNumberOfChildren)
        indexFile.writeNode(halfOfChild)

        node.pointersToMessages.add(id, child.pointersToMessages[middle - 1])
        node.pointersToChildren.add(id + 1, halfOfChild.positionInIndexFile)
        indexFile.writeNode(node)

        child.pointersToMessages.subList(middle - 1, maxNumberOfChildren - 1).clear()
        if (!child.isLeaf())
            child.pointersToChildren.subList(middle, maxNumberOfChildren).clear()
        indexFile.writeNode(child)
    }

    private fun findPositionOfFirstGreater(messages: MutableList<Message>, message: Message): Int {
        val position = messages.binarySearch(message, MessageComparator)
        return -position - 1 // that's how .binarySearch() works
    }

    private fun swapNodes(node1: Node, node2: Node) {
        node1.positionInIndexFile = node2.positionInIndexFile
            .also { node2.positionInIndexFile = node1.positionInIndexFile }
        indexFile.writeNode(node1)
        indexFile.writeNode(node2)
    }

    private fun increaseNumberOfMessages() {
        val currentNumberOfMessages = getCurrentNumberOfMessages()
        messagesFile.seek(0)
        messagesFile.writeInt(currentNumberOfMessages + 1)
    }

    private fun getCurrentNumberOfMessages(): Int {
        messagesFile.seek(0)
        return messagesFile.readInt()
    }
}

class Node(var positionInIndexFile: Long,
           val pointersToMessages: MutableList<Long>,
           val pointersToChildren: MutableList<Long>,
           val maxNumberOfChildren: Int) {

    /*
    var maxNumberOfChildren = 10
    constructor(positionInIndexFile: Long,
                pointersToMessages: MutableList<Long>,
                pointersToChildren: MutableList<Long>,
                maxNumberOfChildren: Int) : this(positionInIndexFile, pointersToMessages, pointersToChildren) {
        this.maxNumberOfChildren = maxNumberOfChildren
    }*/

    fun isLeaf(): Boolean = pointersToChildren.size == 0

    fun isFull(): Boolean = pointersToMessages.size == maxNumberOfChildren - 1

    fun getMessages(messagesFile: RandomAccessFile): MutableList<Message> {
        val messages = mutableListOf<Message>()
        for (pointer in pointersToMessages)
            messages.add(messagesFile.readMessage(pointer))
        return messages
    }

}
/*

class Node(val maxNumberOfChildren: Int,
           val indexFile: RandomAccessFile,
           val messagesFile: RandomAccessFile,
           val positionInIndexFile: Long) {

}*/

/*
class BTree(private val maxNumberOfChildren: Int = 10) {
    var root = Node(mutableListOf(), mutableListOf(), true, maxNumberOfChildren)

    fun addMessage(message: Message) {
        if (root.isFull()) {
            val newRoot = Node(mutableListOf(), mutableListOf(root), false, maxNumberOfChildren)
            newRoot.splitChild(root, 0)
            root = newRoot
        }
        root.insertMessageInNonFull(message)
    }
}

class Node(val messages: MutableList<Message>,
           val children: MutableList<Node>,
           var isLeaf: Boolean,
           val maxNumberOfChildren: Int) {

    fun insertMessageInNonFull(message: Message) {
        val positionToInsert = findPositionOfFirstGreater(message)
        println("$positionToInsert ${messages.size}")
        if (isLeaf)
            messages.add(positionToInsert, message)
        else {
            if (children[positionToInsert].isFull()) {
                splitChild(children[positionToInsert], positionToInsert)
                insertMessageInNonFull(message)
            }
            else
                children[positionToInsert].insertMessageInNonFull(message)
        }
    }

    fun isFull(): Boolean = messages.size == maxNumberOfChildren - 1

    private fun findPositionOfFirstGreater(message: Message): Int {
        val position = messages.binarySearch(message, MessageComparator)
        return -position - 1 // that's how .binarySearch() works
    }

    fun splitChild(child: Node, positionOfChild: Int) {
        val middle = maxNumberOfChildren / 2
        val halfOfChild = Node(
            child.messages.subList(middle, child.messages.size),
            if (child.isLeaf) mutableListOf() else child.children.subList(middle, child.children.size),
            child.isLeaf,
            maxNumberOfChildren
        )
        messages.add(positionOfChild, child.messages[middle - 1])

        child.messages.subList(middle - 1, child.messages.size).clear()
        if (!child.isLeaf)
            child.children.subList(middle, child.children.size).clear()

        children.add(positionOfChild + 1, halfOfChild)
    }
}*/
/*
/**
 * @return first [k] messages that are greater than [message].
 * If there is less than [k] messages after [message], returns all of them.
 */
fun getKNextMessages(bTree: BTree, message: Message, k: Int): List<Message> =
    findKNextMessages(bTree, getRoot(bTree), message, k)

/**
 * @return last [k] messages that are less than [message] in reversed order.
 * If there is less than [k] messages before [message], returns all of them.
 */
fun getKPreviousMessages(bTree: BTree, message: Message, k: Int): List<Message> =
    findKPreviousMessages(bTree, getRoot(bTree), message, k)

/**
 * @return number of stored messages.
 */
fun getNumberOfMessages(bTree: BTree): Int {
    bTree.fileWithIndex.seek(bTree.fileWithIndex.length() - Int.SIZE_BYTES)
    return bTree.fileWithIndex.readInt()
}

/**
 * @return [k] messages from the end.
 * If there is less than [k] messages, returns all messages.
 */
fun getKLastMessages(bTree: BTree, k: Int): List <Message> {
    val lastMessage = getLastMessage(bTree)
    return listOf(lastMessage) + getKPreviousMessages(bTree, lastMessage, k - 1).toMutableList()
}

/**
 * @return last message.
 */
fun getLastMessage(bTree: BTree): Message = findLastMessage(bTree, getRoot(bTree))
*/