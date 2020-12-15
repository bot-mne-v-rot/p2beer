package app

import org.junit.jupiter.api.Test
import ru.emkn.p2beer.app.client.chat.*
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.assertEquals

class MessageClassTests {
    @Test
    fun `check message comparator`() {
        val pk = Random.nextBytes(32)

        var time : Long
        var uid : UShort
        var info : MessageId

        val messages = mutableListOf<Message>()

        for (i in 1 until 100) {
            uid = Random.nextUInt(0u, UShort.MAX_VALUE + 1u).toUShort()
            time = System.currentTimeMillis() - Random.nextInt(0, 5)
            info = MessageId(Random.nextLong(0, 5), time, uid)

            messages.add(Message("", info, pk))
        }

        assertEquals(
            messages.sortedWith(MessageComparator),
            messages.sortedWith(compareBy(
                { it.info.timestamp },
                { it.info.messageID },
                { it.info.twoBytesOfUserID })
            )
        )
    }
}

