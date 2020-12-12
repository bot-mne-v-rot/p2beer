package ru.emkn.p2beer.app.client.chat

import java.io.File
import java.io.RandomAccessFile


fun main() {

}

/**
 * Represent data structure BTree.
 *
 * @property t parameter of tree (each node contains maximum (2 * t - 1) messages)
 * @property pathToFileWithIndex path to file where index of tree will be stored (all information about nodes)
 * @property pathToFileWithMessages path to binary file where messages will be stored
 */
class BTree(val t: Int = 50,
            private val pathToFileWithIndex: String,
            private val pathToFileWithMessages: String) {

    val fileWithIndex: RandomAccessFile = RandomAccessFile(File(pathToFileWithIndex), "rw")
    val fileWithMessages: RandomAccessFile = RandomAccessFile(File(pathToFileWithMessages), "rw")
    var root = Node(0, mutableListOf(), true, mutableListOf(), fileWithIndex, t)
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
class Node(val positionInFile: Long,
           val pointersToMessages: MutableList<Long>,
           var isLeaf: Boolean,
           val pointersToChildren: MutableList<Long>) {

    constructor(positionInFile: Long,
                pointersToMessages: MutableList<Long>,
                isLeaf: Boolean,
                pointersToChildren: MutableList<Long>,
                fileWithIndex: RandomAccessFile,
                t: Int) : this(positionInFile, pointersToMessages, isLeaf, pointersToChildren) {

        writePointers(fileWithIndex, positionInFile, 2 * t - 1, pointersToMessages)
        fileWithIndex.writeBoolean(isLeaf)
        writePointers(fileWithIndex, fileWithIndex.filePointer, 2 * t, pointersToChildren)
    }
}

/**
 * Adds [message] to [bTree].
 */
fun addMessage(bTree: BTree, message: Message) {
    val pointerToMessage = bTree.fileWithMessages.length()
    writeMessage(bTree.fileWithMessages, message)
    if (bTree.root.pointersToMessages.size == bTree.t * 2 - 1) { // root is full -> create new root and split old root
        val newRoot = Node(bTree.fileWithIndex.length(),
            mutableListOf<Long>(),
            false,
            mutableListOf<Long>(bTree.root.positionInFile),
            bTree.fileWithIndex,
            bTree.t)

        splitChild(bTree, newRoot, bTree.root, 0)
        bTree.root = newRoot
        insertMessageInNonFull(bTree, bTree.root, message, pointerToMessage)
    }
    else
        insertMessageInNonFull(bTree, bTree.root, message, pointerToMessage)
}

/**
 * Inserts [message] in [node].
 *
 * It's guaranteed that [node] isn't full.
 * If [node] is leaf then adds [pointerToMessage] to [node].pointersToMessages.
 * Otherwise determines appropriate child and calls function from this child.
 */
fun insertMessageInNonFull(bTree: BTree, node: Node, message: Message, pointerToMessage: Long) {
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    if (node.isLeaf) {
        val position = messagesInThisNode.indexOfFirst { it.compareTo(message) == 1 }
        movePointers(bTree.fileWithIndex, node.positionInFile + position * Long.SIZE_BYTES, node.pointersToMessages.subList(position, node.pointersToMessages.size))
        writeLong(bTree.fileWithIndex, node.positionInFile + position * Long.SIZE_BYTES, pointerToMessage)
    } else {
        var indexOfAppropriateChild =
            messagesInThisNode.indexOfFirst { it.compareTo(message) == 1 } // index of first greater message

        if (indexOfAppropriateChild == -1) // all messages are smaller -> we should go in last child
            indexOfAppropriateChild = messagesInThisNode.size

        var childNode = getNode(bTree.fileWithIndex, node.pointersToChildren[indexOfAppropriateChild], bTree.t)

        if (childNode.pointersToMessages.size == 2 * bTree.t - 1) { // childNode is full -> split it into 2
            val added = splitChild(bTree, node, childNode, indexOfAppropriateChild)
            if (message.compareTo(added.first) == 1)
                childNode = added.second
        }
        insertMessageInNonFull(bTree, childNode, message, pointerToMessage)
    }
}

/**
 * Splits full [child] into 2 nodes: first half of [child] and newNode.
 *
 * Leaves first (t - 1) messages and t children in [child].
 * Adds last (t - 1) messages and t children into newNode.
 * Inserts pointerToMiddleMessage and newNode into [node].
 *
 * @return pointerToMiddleMessage and newNode
 * It is necessary because after applying function
 * node contains old information.
 */
fun splitChild(bTree: BTree, node: Node, child: Node, id: Int): Pair<Message, Node> {
    val messagesOfNewNode = child.pointersToMessages.subList(bTree.t, child.pointersToMessages.size)
    val childrenOfNewNode = if (child.isLeaf) mutableListOf<Long>() else child.pointersToChildren.subList(bTree.t, child.pointersToChildren.size)
    val newNode = Node(bTree.fileWithIndex.length(),
        messagesOfNewNode,
        child.isLeaf,
        childrenOfNewNode,
        bTree.fileWithIndex,
        bTree.t)

    movePointers(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES, // pointerToMiddleMessage goes on id position ->
        node.pointersToMessages.subList(id, node.pointersToMessages.size)) // -> we should move all messages starting from id

    val pointerToMiddleMessage = child.pointersToMessages[bTree.t]

    writeLong(bTree.fileWithIndex, node.positionInFile + node.positionInFile + id * Long.SIZE_BYTES,
        pointerToMiddleMessage)

    val positionOfFirstChild = node.positionInFile + (2 * bTree.t - 1) * Long.SIZE_BYTES + 1

    movePointers(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES, // newNode goes on (id + 1) position ->
                    node.pointersToChildren.subList(id + 1, node.pointersToChildren.size)) // -> we should move all children starting from (id + 1)

    writeLong(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES, newNode.positionInFile)

    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t - 1) * Long.SIZE_BYTES, bTree.t) // delete snd half of messages from child (they went to newNode)
    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t * 3 - 1) * Long.SIZE_BYTES + 1, bTree.t) // delete snd half of children from child (they went to newNode)

    return Pair(readMessage(bTree.fileWithMessages, pointerToMiddleMessage), newNode)
}