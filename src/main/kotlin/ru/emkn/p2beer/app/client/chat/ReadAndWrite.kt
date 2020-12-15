package ru.emkn.p2beer.app.client.chat

import java.io.RandomAccessFile

fun readMessage(fileWithMessages: RandomAccessFile, start: Long): Message {
    fileWithMessages.seek(start)

    val buffer = ByteArray(fileWithMessages.readInt())
    fileWithMessages.read(buffer)

    val text = buffer.decodeToString()

    val messageId = fileWithMessages.readLong()
    val timeStamp = fileWithMessages.readLong()
    val twoBytesOfUserId = ((fileWithMessages.read() shl 8) or fileWithMessages.read()).toUShort()

    val info = MessageId(messageId, timeStamp, twoBytesOfUserId)

    val sender = ByteArray(32)
    fileWithMessages.read(sender)

    return Message(text, info, sender)
}

/**
 * @return list of messages stored in [node]
 */
fun getMessages(node: Node, fileWithMessages: RandomAccessFile): List<Message> {
    val messages = mutableListOf<Message>()
    node.pointersToMessages.forEach {
        messages.add(readMessage(fileWithMessages, it))
    }
    return messages
}

fun writeMessage(fileWithMessages: RandomAccessFile, message: Message) {
    fileWithMessages.seek(fileWithMessages.length())

    fileWithMessages.writeInt(message.text.toByteArray().size)
    fileWithMessages.write(message.text.toByteArray())

    fileWithMessages.writeLong(message.info.messageID)
    fileWithMessages.writeLong(message.info.timestamp)

    fileWithMessages.write(message.info.twoBytesOfUserID.toInt() shr 8)
    fileWithMessages.write(message.info.twoBytesOfUserID.toInt() shr 0)

    fileWithMessages.write(message.sender)
}

/**
 * Read pointers from [fileWithIndex].
 *
 * From some position pointers can equal to -1,
 * which means that they are still uninitialized
 * so we should ignore them.
 */
fun readPointers(fileWithIndex: RandomAccessFile, start: Long, maxNumberOfPointers: Int): MutableList<Long> {
    fileWithIndex.seek(start)
    val pointers = mutableListOf<Long>()
    for (i in 1..maxNumberOfPointers) {
        val pointer = fileWithIndex.readLong()
        if (pointer < 0)
            break
        else
            pointers.add(pointer)
    }
    return pointers
}

fun writePointers(fileWithIndex: RandomAccessFile, start: Long, maxNumberOfPointers: Int, pointers: List<Long>) {
    fileWithIndex.seek(start)
    pointers.forEach {
        fileWithIndex.writeLong(it)
    }
    repeat(maxNumberOfPointers - pointers.size) {
        fileWithIndex.writeLong(-1)
    }
}

/**
 * Move [pointers] to right.
 *
 * For example: 1 2 3 4 -> _ 1 2 3 4.
 * [start] defines the position of first pointer,
 * other go right after it.
 */
fun movePointers(fileWithIndex: RandomAccessFile, start: Long, pointers: List<Long>) {
    for (i in pointers.size - 1 downTo 0)
        writeLong(fileWithIndex, start + (i + 1) * Long.SIZE_BYTES, pointers[i])
}

/**
 * Fills [number] Long values with -1.
 *
 */
fun deletePointers(fileWithIndex: RandomAccessFile, start: Long, number: Int) {
    fileWithIndex.seek(start)
    repeat(number) {
        fileWithIndex.writeLong(-1)
    }
}

fun readBoolean(fileWithIndex: RandomAccessFile, start: Long): Boolean {
    fileWithIndex.seek(start)
    return fileWithIndex.readBoolean()
}

/**
 * Starting with [start] position in [fileWithIndex] we have our node:
 *  -> 2 * t - 1 Long values - pointers to messages
 *  -> one Boolean values - isLeaf
 *  -> 2 * t Long values - pointers to children.
 */
fun getNode(bTree: BTree, start: Long): Node {
    return Node(start,
            readPointers(bTree.fileWithIndex, start, 2 * bTree.t - 1),
            readBoolean(bTree.fileWithIndex, start + (2 * bTree.t - 1) * Long.SIZE_BYTES),
            readPointers(bTree.fileWithIndex, bTree.fileWithIndex.filePointer, 2 * bTree.t))
}

fun getRoot(bTree: BTree): Node = getNode(bTree, 0)

fun swapNodes(fileWithIndex: RandomAccessFile, node1: Node, node2: Node, t: Int) {
    val position1 = node1.positionInFile
    val position2 = node2.positionInFile

    writePointers(fileWithIndex, position1, 2 * t - 1, node2.pointersToMessages)
    fileWithIndex.writeBoolean(node2.isLeaf)
    writePointers(fileWithIndex, fileWithIndex.filePointer, 2 * t, node2.pointersToChildren)

    writePointers(fileWithIndex, position2, 2 * t - 1, node1.pointersToMessages)
    fileWithIndex.writeBoolean(node1.isLeaf)
    writePointers(fileWithIndex, fileWithIndex.filePointer, 2 * t, node1.pointersToChildren)
}

fun writeLong(file: RandomAccessFile, start: Long, value: Long) {
    file.seek(start)
    file.writeLong(value)
}

fun writeInt(file: RandomAccessFile, start: Long, value: Int) {
    file.seek(start)
    file.writeInt(value)
}

fun printBTree(bTree: BTree, node: Node) {
    println("There are ${node.pointersToMessages.size} messages in node ${node.positionInFile}")
    for (pointerToMessage in node.pointersToMessages) {
        val message = readMessage(bTree.fileWithMessages, pointerToMessage)
        println(message.info.timestamp)
    }
    println("There are ${node.pointersToChildren.size} children in node ${node.positionInFile}")
    for (pointerToChild in node.pointersToChildren) {
        println(pointerToChild)
    }

    for (pointerToChild in node.pointersToChildren) {
        printBTree(bTree, getNode(bTree, pointerToChild))
    }
}