package ru.emkn.p2beer.app.client.chat

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/*
fun main() {
    val a = RandomAccessFile(File("test.bin"), "rw")
    a.writeInt(228)
    a.writeInt(337)
    a.seek(0)
    println(a.writeInt(12))
    a.seek(0)
    println(a.readInt())

    println(a.readInt())
}
*/

class BTree(var pointerToRoot: Long = 0,
            val t: Int = 50,
            private val pathToFileWithIndex: String,
            private val pathToFileWithMessages: String) {
    val fileWithIndex: RandomAccessFile = RandomAccessFile(File(pathToFileWithIndex), "rw")
    val fileWithMessages: RandomAccessFile = RandomAccessFile(File(pathToFileWithMessages), "rw")
    init {
        repeat(2 * t - 1) {
            fileWithIndex.writeInt(-1)
        }
        fileWithIndex.writeBoolean(true)
        repeat(2 * t) {
            fileWithIndex.writeInt(-1)
        }
    }
}


data class Node(val pointersToMessages: MutableList<Long>, var isLeaf: Boolean, val pointersToChildren: MutableList<Long>)

/**
 * Read pointers from [fileWithIndex].
 *
 * From some position pointers can equal to -1,
 * which means that they are still uninitialized
 * so we should stop reading.
 */
fun readPointers(fileWithIndex: RandomAccessFile, start: Long, maxNumberOfPointers: Int): MutableList<Long> {
    fileWithIndex.seek(start)
    val pointers = mutableListOf<Long>()
    for (_ in 1..maxNumberOfPointers) {
        val pointer = fileWithIndex.readLong()
        if (pointer < 0) break else pointers.add(pointer)
    }
    return pointers
}

/**
 * Starting with [start] position in [fileWithIndex] we have our node:
 *  -> 2[t] - 1 Long values - pointers to messages
 *  -> one Boolean values - isLeaf
 *  -> 2[t] Long values - pointers to children.
 */
fun getNodeFromFile(fileWithIndex: RandomAccessFile, start: Long, t: Int): Node {
    return Node(readPointers(fileWithIndex, start, 2 * t - 1),
                fileWithIndex.readBoolean(),
                readPointers(fileWithIndex, fileWithIndex.filePointer, 2 * t))
}

fun readStringFromFile(file: RandomAccessFile, start: Long): String {
    file.seek(start)
    val length = file.readInt()
    val buffer = ByteArray(length)
    file.read(buffer)
    return buffer.decodeToString()
}

/**
 * @return list of messages stored in [node]
 * from [fileWithMessages] that contains all messages.
 */
fun getMessagesFromNode(node: Node, fileWithMessages: RandomAccessFile): List<Message> {
    val messages = mutableListOf<Message>()
    node.pointersToMessages.forEach {
        val text = readStringFromFile(fileWithMessages, it)
        val messageId = fileWithMessages.readLong()
        val timeStamp = fileWithMessages.readLong()
        val twoBytesOfUserId = ((fileWithMessages.readUnsignedByte() shl 8) or fileWithMessages.readUnsignedByte()).toUShort()
        val info = MessageId(messageId, timeStamp, twoBytesOfUserId)
        val publicKey = TODO()
        messages.add(Message(text, info, publicKey))
    }
    return messages
}






        /*

fun addKey(bTree: BTree, key: Message) {
    if (bTree.root.keys.size == bTree.t * 2 - 1) {
        val newRoot = Node(keys = mutableListOf(), isLeaf = false, children = mutableListOf(bTree.root))
        bTree.root = newRoot
        splitChild(bTree.root, bTree.t, 0)
    }
    insertNonFull(bTree.root, key, bTree.t)
}

fun insertNonFull(node: Node, key: Message, t: Int) {
    if (node.isLeaf) {
        val positionToInsert = node.keys.indexOfFirst { it.compareTo(key) == 1 }
        if (positionToInsert == -1)
            node.keys.add(key)
        else
            node.keys.add(positionToInsert, key)
        // disk write node
    }
    else {
        val suitableChild = node.children.find
    }

}

fun splitChild(node: Node, t: Int, idOfChild: Int) {

}*/