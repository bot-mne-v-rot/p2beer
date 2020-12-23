package ru.emkn.p2beer.app.client.chat

import java.io.File
import java.io.RandomAccessFile

/**
 * Represents data structure BTree.
 *
 * @property t parameter of tree (each node contains maximum (2 * t - 1) messages)
 * @property pathToFileWithIndex path to file where index of tree will be stored (all information about nodes)
 * @property pathToFileWithMessages path to binary file where messages will be stored
 *
 * To create an instance: val bTree = BTree(pathToFileWithIndex = "...", pathToFileWithMessages = "...")
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
 * Represents node in BTree.
 *
 * @property positionInFile position in fileWithIndex where description of node starts
 * @property pointersToMessages pointers to messages that consists node (messages are stored in fileWithMessages)
 * @property isLeaf leaf checker
 * @property pointersToChildren pointers to children of node
 *
 * Secondary constructor reserves (2 * t - 1) * 8 bytes for pointers to messages,
 * 1 byte for isLeaf and (2 * t) bytes for pointers to children.
 * Then it fills some of these bytes with actual information.
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
 */
fun addMessage(bTree: BTree, message: Message) {
    val pointerToMessage = bTree.fileWithMessages.length()
    writeMessage(bTree.fileWithMessages, message)

    val numberOfMessages = getNumberOfMessages(bTree)
    writeInt(bTree.fileWithIndex, bTree.fileWithIndex.length() - Int.SIZE_BYTES, numberOfMessages + 1)

    val root = getRoot(bTree)

    if (root.pointersToMessages.size == bTree.t * 2 - 1) { // root is full -> create new root and split old root
        var newRoot = Node(bTree.fileWithIndex.length() - Int.SIZE_BYTES,
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

fun getKNextMessages(bTree: BTree, message: Message, k: Int): List<Message> =
        findKNextMessages(bTree, getRoot(bTree), message, k, k)


fun getKPreviousMessages(bTree: BTree, message: Message, k: Int): List<Message> =
        findKPreviousMessages(bTree, getRoot(bTree), message, k, k)

fun getNumberOfMessages(bTree: BTree): Int {
    bTree.fileWithIndex.seek(bTree.fileWithIndex.length() - Int.SIZE_BYTES)
    return bTree.fileWithIndex.readInt()
}

fun getKLastMessages(bTree: BTree, k: Int): List <Message> =
        getKPreviousMessages(bTree, getLastMessage(bTree), k)

fun getLastMessage(bTree: BTree): Message = findLastMessage(bTree, getRoot(bTree))