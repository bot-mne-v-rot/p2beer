package ru.emkn.p2beer.app.client.chat

import ru.emkn.p2beer.app.client.user.*
import kotlin.Comparator

data class Message(
        val text: String,
        val info: MessageId,
        val sender: PublicKey
) {
    override fun toString() = text

    operator fun compareTo(other: Message) : Int = this.info.compareTo(other.info)
}

data class MessageId (
        val messageID: Long,
        val timestamp: Long,
        val twoBytesOfUserID: UShort
) {
    operator fun compareTo(other: MessageId) : Int {

        /**
         * MessageId is compared by timestamp firstly, then messageID, then userID.
         * @return 1 if message is counted newer than the one passed in
         * @param other
         * Otherwise
         * @return -1
         */

        return when {
            this.timestamp > other.timestamp -> 1
            this.timestamp < other.timestamp -> -1
            this.messageID > other.messageID -> 1
            this.messageID < other.messageID -> -1
            else -> if (this.twoBytesOfUserID > other.twoBytesOfUserID) 1 else -1
        }
    }
}

class MessageComparator {

    companion object : Comparator<Message?> {

        override fun compare(o1: Message?, o2: Message?) : Int {
            return when {
                (o1 === o2) -> 0
                (o1 == null) -> -1
                (o2 == null) -> 1
                else -> o1.compareTo(o2)
            }
        }

    }
}

