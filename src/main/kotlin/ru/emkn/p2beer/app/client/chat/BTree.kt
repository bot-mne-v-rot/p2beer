package ru.emkn.p2beer.app.client.chat

import java.io.File
import java.io.RandomAccessFile

/**
 * Represents data structure BTree.
 *
 * @property t parameter of tree (each node contains maximum (2 * t - 1) messages)
 * @property pathToFileWithIndex path to file where index of tree will be stored (all information about nodes)
 * @property pathToFileWithMessages path to binary file where messages will be stored
 */
class BTree(val t: Int,
            private val pathToFileWithIndex: String,
            private val pathToFileWithMessages: String) {

    val fileWithIndex: RandomAccessFile = RandomAccessFile(File(pathToFileWithIndex), "rw")
    val fileWithMessages: RandomAccessFile = RandomAccessFile(File(pathToFileWithMessages), "rw")
    var pointerToRoot: Long = 0

    init {
        fileWithMessages.setLength(0)
        fileWithIndex.setLength(0)
        Node(pointerToRoot, mutableListOf(), true, mutableListOf(), fileWithIndex, t)
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

    val root = getNode(bTree.fileWithIndex, bTree.pointerToRoot, bTree.t)

    if (root.pointersToMessages.size == bTree.t * 2 - 1) { // root is full -> create new root and split old root
        var newRoot = Node(bTree.fileWithIndex.length(),
            mutableListOf(),
            false,
            mutableListOf(root.positionInFile),
            bTree.fileWithIndex,
            bTree.t)

        val checkNewRoot = getNode(bTree.fileWithIndex, newRoot.positionInFile, bTree.t)

        val realNewRoot = splitChild(bTree, newRoot, root, 0)
        bTree.pointerToRoot = realNewRoot.positionInFile
        insertMessageInNonFull(bTree, realNewRoot, message, pointerToMessage)
    }
    else
        insertMessageInNonFull(bTree, root, message, pointerToMessage)
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

    var position = messagesInThisNode.binarySearch(message, MessageComparator)
    // index of first message that is newer
    position = -position - 1

    if (node.isLeaf) {
        movePointers(bTree.fileWithIndex, node.positionInFile + position * Long.SIZE_BYTES,
            node.pointersToMessages.subList(position, node.pointersToMessages.size))

        writeLong(bTree.fileWithIndex, node.positionInFile + position * Long.SIZE_BYTES, pointerToMessage)
    }
    else {
        val childNode = getNode(bTree.fileWithIndex, node.pointersToChildren[position], bTree.t)

        if (childNode.pointersToMessages.size == 2 * bTree.t - 1) { // childNode is full -> split it into 2
            val updatedNode = splitChild(bTree, node, childNode, position)
            insertMessageInNonFull(bTree, updatedNode, message, pointerToMessage)
        }
        else
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
fun splitChild(bTree: BTree, node: Node, child: Node, id: Int): Node {

    val messagesOfNewNode = child.pointersToMessages.subList(bTree.t, child.pointersToMessages.size)

    val childrenOfNewNode = if
                                    (child.isLeaf) mutableListOf<Long>() else child.pointersToChildren
            .subList(bTree.t, child.pointersToChildren.size)
    val newNode = Node(bTree.fileWithIndex.length(),
        messagesOfNewNode,
        child.isLeaf,
        childrenOfNewNode,
        bTree.fileWithIndex,
        bTree.t)

    movePointers(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES,
            // pointerToMiddleMessage goes on id position ->
        node.pointersToMessages.subList(id, node.pointersToMessages.size))
    // -> we should move all messages starting from id

    val pointerToMiddleMessage = child.pointersToMessages[bTree.t - 1]

    node.pointersToMessages.add(id, pointerToMiddleMessage)

    writeLong(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES,
        pointerToMiddleMessage)

    val positionOfFirstChild = node.positionInFile + (2 * bTree.t - 1) * Long.SIZE_BYTES + 1

    movePointers(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES,
            // newNode goes on (id + 1) position ->
        node.pointersToChildren.subList(id + 1, node.pointersToChildren.size))
    // -> we should move all children starting from (id + 1)

    node.pointersToChildren.add(id + 1, newNode.positionInFile)

    writeLong(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES, newNode.positionInFile)

    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t - 1) * Long.SIZE_BYTES, bTree.t)
    // delete snd half of messages from child (they went to newNode)
    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t * 3 - 1) * Long.SIZE_BYTES + 1, bTree.t)
    // delete snd half of children from child (they went to newNode)

    return node
}

fun getKMessages(bTree: BTree, message: Message, k: Int): List<Message> {
    val root = getNode(bTree.fileWithIndex, bTree.pointerToRoot, bTree.t)
    return findMessages(bTree, root, message, k, k)
}

fun findMessages(bTree: BTree, node: Node, message: Message, k: Int, amountLeft: Int): List<Message> {
    if (amountLeft <= 0)
        return listOf()
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    val messages = mutableListOf<Message>()

    if (node.isLeaf) {
        var position = messagesInThisNode.binarySearch(message, MessageComparator)

        if (position < 0)
            position = -position - 1

        return messagesInThisNode.subList(position,
            position + minOf(messagesInThisNode.size - position, amountLeft))
    }
    else {
        var indexOfGoodChild = 0

        if (amountLeft == k) {
            var position = messagesInThisNode.binarySearch(message, MessageComparator)
            // index of first message that is newer

            if (position >= 0) {
                messages.add(message)
            }
            else {
                position = -position - 1

                val childNode = getNode(bTree.fileWithIndex, node.pointersToChildren[position], bTree.t)

                messages += findMessages(bTree, childNode, message, k, amountLeft)

                if (messages.size < k && position < messagesInThisNode.size)
                    messages.add(messagesInThisNode[position])
            }

            if (messages.size == k)
                return messages
            else
                indexOfGoodChild = position + 1
        }

        for (index in indexOfGoodChild until node.pointersToChildren.size) {
            val pointer = node.pointersToChildren[index]
            val child = getNode(bTree.fileWithIndex, pointer, bTree.t)

            messages += findMessages(bTree, child, message, k, amountLeft - messages.size)

            if (messages.size < amountLeft && index != node.pointersToChildren.lastIndex)
                messages.add(messagesInThisNode[index])

            if (messages.size == amountLeft)
                break
        }
    }

    return messages
}