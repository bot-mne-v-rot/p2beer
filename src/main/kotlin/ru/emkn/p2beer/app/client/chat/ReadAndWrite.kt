package ru.emkn.p2beer.app.client.chat

import java.io.RandomAccessFile

fun RandomAccessFile.readMessage(position: Long): Message {
    this.seek(position)

    val buffer = ByteArray(this.readInt())
    this.read(buffer)
    val text = buffer.decodeToString()

    val messageId = this.readLong()
    val timeStamp = this.readLong()
    val twoBytesOfUserId = ((this.read() shl 8) or this.read()).toUShort()

    val info = MessageId(messageId, timeStamp, twoBytesOfUserId)

    val sender = ByteArray(32)
    this.read(sender)

    return Message(text, info, sender)
}

fun RandomAccessFile.writeMessageToTheEnd(message: Message): Long {
    val position = this.length()
    this.seek(position)

    this.writeInt(message.text.toByteArray().size)
    this.write(message.text.toByteArray())

    this.writeLong(message.info.messageID)
    this.writeLong(message.info.timestamp)

    this.write(message.info.twoBytesOfUserID.toInt() shr 8)
    this.write(message.info.twoBytesOfUserID.toInt() shr 0)

    this.write(message.sender)
    return position
}

fun RandomAccessFile.readPointers(position: Long, maxNumberOfPointers: Int): MutableList<Long> {
    this.seek(position)
    val pointers = mutableListOf<Long>()
    for (i in 0 until maxNumberOfPointers) {
        val pointer = this.readLong()
        if (pointer < 0) break else pointers.add(pointer)
    }
    return pointers
}

fun RandomAccessFile.readNode(position: Long, maxNumberOfChildren: Int): Node =
    Node(position,
        this.readPointers(position, maxNumberOfChildren - 1),
        this.readPointers(position + (maxNumberOfChildren - 1) * Long.SIZE_BYTES, maxNumberOfChildren),
        maxNumberOfChildren)

fun RandomAccessFile.writePointers(pointers: MutableList<Long>, maxNumberOfPointers: Int) {
    for (i in 0 until maxNumberOfPointers)
        this.writeLong(if (i < pointers.size) pointers[i] else -1)
}

fun RandomAccessFile.writeNode(node: Node) {
    this.seek(node.positionInIndexFile)
    this.writePointers(node.pointersToMessages, node.maxNumberOfChildren - 1)
    this.writePointers(node.pointersToChildren, node.maxNumberOfChildren)
}

/*
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
*/