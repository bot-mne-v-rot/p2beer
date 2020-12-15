package app

import chat.getRandomString
import org.junit.jupiter.api.Test
import ru.emkn.p2beer.app.client.user.Friend
import ru.emkn.p2beer.app.client.user.FriendComparator
import ru.emkn.p2beer.app.client.user.UserInfo
import kotlin.random.Random
import kotlin.test.assertEquals

private fun createFriend() : Friend {
    val pk = Random.nextBytes(32)
    val userName = getRandomString(9)
    val time = Random.nextLong(0, 100000)
    val onlineStatus = Random.nextBoolean()

    val uInfo = UserInfo(pk, userName, time, onlineStatus)

    val messageCount = Random.nextLong(0,1000)
    val isConnected = Random.nextBoolean()
    val lastMTime = Random.nextLong(0,1000)

    return Friend(uInfo, isConnected, messageCount, lastMTime)
}

class FriendClassTests {
    @Test
    fun `check friend comparator`() {

        val friends = mutableListOf<Friend>()

        for (i in 1 until 100)
            friends.add(createFriend())

        assertEquals(
            friends.sortedWith(FriendComparator),
                friends.sortedWith(compareByDescending { it.lastMessageTimeStamp })
        )
        println(friends.sortedWith(FriendComparator))
    }
}

