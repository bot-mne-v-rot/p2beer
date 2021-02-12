package ru.emkn.p2beer.app.client.chat.bTree

import ru.emkn.p2beer.app.client.chat.Message
import ru.emkn.p2beer.app.client.chat.MessageId
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

fun RandomAccessFile.readNode(position: Long, maxNumberOfChildren: Int,
                              indexFile: RandomAccessFile, messagesFile: RandomAccessFile): Node =
    Node(position,
        this.readPointers(position, maxNumberOfChildren - 1),
        this.readPointers(position + (maxNumberOfChildren - 1) * Long.SIZE_BYTES, maxNumberOfChildren),
        maxNumberOfChildren, indexFile, messagesFile)

fun RandomAccessFile.writePointers(pointers: MutableList<Long>, maxNumberOfPointers: Int) {
    for (i in 0 until maxNumberOfPointers)
        this.writeLong(if (i < pointers.size) pointers[i] else -1)
}

fun RandomAccessFile.writeNode(node: Node) {
    this.seek(node.positionInIndexFile)
    this.writePointers(node.pointersToMessages, node.maxNumberOfChildren - 1)
    this.writePointers(node.pointersToChildren, node.maxNumberOfChildren)
}
