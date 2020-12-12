package ru.emkn.p2beer.app.client.chat

import java.io.RandomAccessFile

fun readString(file: RandomAccessFile, length: Int): String {
    val buffer = ByteArray(length)
    file.read(buffer)
    return buffer.decodeToString()
}

fun writeString(file: RandomAccessFile, start: Long, string: String) {
    file.seek(start)
    file.write(string.toByteArray())
}

/**
 * Read one message from [fileWithMessages].
 *
 * [start] defines the position of message in file.
 */
fun readMessage(fileWithMessages: RandomAccessFile, start: Long): Message {
    fileWithMessages.seek(start)
    val text = readString(fileWithMessages, fileWithMessages.readInt())
    val messageId = fileWithMessages.readLong()
    val timeStamp = fileWithMessages.readLong()
    val twoBytesOfUserId = ((fileWithMessages.read() shl 8) or fileWithMessages.read()).toUShort()
    val info = MessageId(messageId, timeStamp, twoBytesOfUserId)
    val sender = TODO()
    return Message(text, info, sender)
}

fun writeMessage(fileWithMessages: RandomAccessFile, message: Message) {
    writeString(fileWithMessages, fileWithMessages.length(), message.text)
    fileWithMessages.writeLong(message.info.messageID)
    fileWithMessages.writeLong(message.info.timestamp)
    fileWithMessages.write(message.info.twoBytesOfUserID.toInt() shr 8)
    fileWithMessages.write(message.info.twoBytesOfUserID.toInt() shr 0)
    TODO("Write publicKey")
}

/**
 * @return list of messages stored in [node] from [fileWithMessages]
 * that contains all messages.
 */
fun getMessages(node: Node, fileWithMessages: RandomAccessFile): List<Message> {
    val messages = mutableListOf<Message>()
    node.pointersToMessages.forEach {
        messages.add(readMessage(fileWithMessages, it))
    }
    return messages
}

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
    for (i in 1..maxNumberOfPointers) {
        val pointer = fileWithIndex.readLong()
        if (pointer < 0) break else pointers.add(pointer)
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
 * Move [pointers] in [fileWithIndex] to right.
 *
 * For example: 1 2 3 4 -> _ 1 2 3 4.
 * [start] defines the position of first pointer,
 * other go right after him.
 */
fun movePointers(fileWithIndex: RandomAccessFile, start: Long, pointers: List<Long>) {
    for (i in pointers.size - 1 downTo 0)
        writeLong(fileWithIndex, start + (i + 1) * Long.SIZE_BYTES, pointers[i])
}

fun deletePointers(fileWithIndex: RandomAccessFile, start: Long, number: Int) {
    fileWithIndex.seek(start)
    repeat(number) {
        fileWithIndex.writeLong(-1)
    }
}

/**
 * Starting with [start] position in [fileWithIndex] we have our node:
 *  -> 2[t] - 1 Long values - pointers to messages
 *  -> one Boolean values - isLeaf
 *  -> 2[t] Long values - pointers to children.
 */
fun getNode(fileWithIndex: RandomAccessFile, start: Long, t: Int): Node {
    return Node(start,
        readPointers(fileWithIndex, start, 2 * t - 1),
        fileWithIndex.readBoolean(),
        readPointers(fileWithIndex, fileWithIndex.filePointer, 2 * t))
}

fun writeLong(file: RandomAccessFile, start: Long, value: Long) {
    file.seek(start)
    file.writeLong(value)
}