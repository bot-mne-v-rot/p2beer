package app

import org.junit.jupiter.api.Test
import ru.emkn.p2beer.app.client.user.*
import kotlin.random.Random
import kotlin.test.assertEquals

class SaveDataTests {
    @Test
    fun `account data save test`() {
        val pk = Random.nextBytes(32)
        val pr = Random.nextBytes(32)

        val lastSeen = System.currentTimeMillis()
        val userInfo = UserInfo(pk, "KDizzled", lastSeen, false)

        val friendInfo1 = UserInfo(pk, "Mimimaxik", lastSeen, false)
        val friendInfo2 = UserInfo(pk, "SmnTin", lastSeen, false)

        val friend1 = Friend(friendInfo1, true,
                Random.nextInt(0, 100000),
                Random.nextLong(0, 100000))
        val friend2 = Friend(friendInfo2, false,
                Random.nextInt(0, 100000),
                Random.nextLong(0, 100000))

        val me = Account(userInfo, pr, setOf(friend1, friend2))

        val dataStorage : UserDataStorage = JSONUserDataStorageImpl()

        dataStorage.saveMyData(me)
        assertEquals(me, dataStorage.loadMyData())
    }
}