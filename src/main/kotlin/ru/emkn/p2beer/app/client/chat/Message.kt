package ru.emkn.p2beer.app.client.chat

import ru.emkn.p2beer.app.client.user.*

data class Message(
        val text: String,
        val info: MessageId,
        val sender: ByteArray
) {

    override fun toString() = text

    operator fun compareTo(other: Message) : Int = this.info.compareTo(other.info)

    override fun equals(other: Any?): Boolean {
        return if (other == null || other !is Message)
            false
        else
            this.compareTo(other) == 0
    }

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
            this.twoBytesOfUserID > other.twoBytesOfUserID -> 1
            this.twoBytesOfUserID < other.twoBytesOfUserID -> -1
            else -> 0
        }
    }
}

class MessageComparator {

    companion object : Comparator<Message?> {

        override fun compare(o1: Message?, o2: Message?) : Int {
            return when {
                (o1 == o2) -> 0
                (o1 == null) -> -1
                (o2 == null) -> 1
                else -> o1.compareTo(o2)
            }
        }
    }
}

