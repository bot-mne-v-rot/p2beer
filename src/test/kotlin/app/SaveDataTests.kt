package app

import org.junit.jupiter.api.Test
import ru.emkn.p2beer.app.client.user.*
import kotlin.random.Random

class SaveDataTests {
    @Test
    fun `account data save test`() {
        val pk = Random.nextBytes(32)
        val pr = Random.nextBytes(32)

        val lastSeen = System.currentTimeMillis()
        val userInfo = UserInfo(pk, "KDizzled", lastSeen, false)

        val friendInfo1 = UserInfo(pk, "Mimimaxik", lastSeen, false)
        val friendInfo2 = UserInfo(pk, "SmnTin", lastSeen, false)

        val friend1 = Friend(friendInfo1, true)
        val friend2 = Friend(friendInfo2, false)

        val me = Account(userInfo, pr, setOf(friend1, friend2))

        val dataStorage : UserDataStorage = JSONUserDataStorageImpl()

        dataStorage.saveMyData(me)
        println(dataStorage.loadMyData())
    }
}