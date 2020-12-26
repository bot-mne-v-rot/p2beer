package ru.emkn.p2beer.app.client.chat

import java.io.File
import java.io.RandomAccessFile

/**
 * Represents BTree data structure.
 * Main operations:
 *  - save message to disk.
 *  - get k consecutive messages starting from specific one.
 *
 * @property t parameter of tree (each node contains maximum (2 * t - 1) messages).
 * @property pathToFileWithIndex path to file where index of tree will be stored (all information about nodes).
 * @property pathToFileWithMessages path to binary file where messages will be stored.
 *
 * To create an instance: val bTree = BTree(pathToFileWithIndex = "...", pathToFileWithMessages = "...").
 */
class BTree(val t: Int = 5,
            private val pathToFileWithIndex: String,
            private val pathToFileWithMessages: String) {

    private val exists = File(pathToFileWithIndex).exists()

    val fileWithIndex = RandomAccessFile(File(pathToFileWithIndex), "rw")
    val fileWithMessages = RandomAccessFile(File(pathToFileWithMessages), "rw")

    init {
        if (exists) {
            fileWithIndex.seek(fileWithIndex.length())
            fileWithMessages.seek(fileWithMessages.length())
        }
        else {
            fileWithIndex.writeInt(0) // last 4 bytes store number of messages in BTree
            Node(0, mutableListOf(), true, mutableListOf(), this)
        }
    }
}

/**
 * Represents a node in BTree.
 *
 * Each node is just a sequence of bytes
 * starting from [positionInFile] in specific order:
 *  - (2 * t - 1) * 8 bytes are reserved for [pointersToMessages],
 *  - 1 byte for [isLeaf] checker.
 *  - (2 * t) * 8 bytes are reserved for [pointersToChildren].
 */
class Node(var positionInFile: Long,
           val pointersToMessages: MutableList<Long>,
           var isLeaf: Boolean,
           val pointersToChildren: MutableList<Long>) {

    constructor(positionInFile: Long,
                pointersToMessages: MutableList<Long>,
                isLeaf: Boolean,
                pointersToChildren: MutableList<Long>,
                bTree: BTree) : this(positionInFile, pointersToMessages, isLeaf, pointersToChildren) {

        val currentNumberOfMessages = getNumberOfMessages(bTree) // it will be overwritten by new Node

        writePointers(bTree.fileWithIndex, positionInFile, 2 * bTree.t - 1, pointersToMessages)
        bTree.fileWithIndex.writeBoolean(isLeaf)
        writePointers(
                bTree.fileWithIndex,
                bTree.fileWithIndex.filePointer,
                2 * bTree.t,
                pointersToChildren
        )

        bTree.fileWithIndex.writeInt(currentNumberOfMessages)
    }
}

/**
 * Adds [message] to [bTree].
 *
 * Writes [message] to the end of file with messages
 * and starts recursive search of appropriate node to insert node.
 */
fun addMessage(bTree: BTree, message: Message) {
    val pointerToMessage = bTree.fileWithMessages.length()
    writeMessage(bTree.fileWithMessages, message)

    val numberOfMessages = getNumberOfMessages(bTree)
    writeInt(bTree.fileWithIndex, bTree.fileWithIndex.length() - Int.SIZE_BYTES, numberOfMessages + 1)

    val root = getRoot(bTree)

    if (root.pointersToMessages.size == bTree.t * 2 - 1) { // root is full -> create new root and split old root
        val newRoot = Node(bTree.fileWithIndex.length() - Int.SIZE_BYTES,
                mutableListOf(),
                false,
                mutableListOf(root.positionInFile),
                bTree)

        val realNewRoot = splitChild(bTree, newRoot, root, 0)
        val updatedRoot = getRoot(bTree)                                // just
        realNewRoot.pointersToChildren[0] = realNewRoot.positionInFile         // magic

        swapNodes(bTree.fileWithIndex, updatedRoot, realNewRoot, bTree.t)
        // root always must be first in file -> swap old root and new root
        realNewRoot.positionInFile = 0 // it's new root

        insertMessageInNonFull(bTree, realNewRoot, message, pointerToMessage)
    }
    else
        insertMessageInNonFull(bTree, root, message, pointerToMessage)
}

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
