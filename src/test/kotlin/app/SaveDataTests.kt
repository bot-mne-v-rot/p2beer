package app

import chat.getRandomString
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import ru.emkn.p2beer.app.client.user.*
import kotlin.random.Random
import kotlin.test.assertEquals
import ru.emkn.p2beer.app.client.util.*

class SaveDataTests {
    @Test
    fun `account data save test`() {
        val pk1 = Random.nextBytes(32)
        val pk2 = Random.nextBytes(32)
        val pk3 = Random.nextBytes(32)
        val pr = Random.nextBytes(32)

        val lastSeen = System.currentTimeMillis()
        val userInfo = UserInfo(pk1, "KDizzled", lastSeen, false)

        val friendInfo1 = UserInfo(pk2, "Mimimaxik", lastSeen, false)
        val friendInfo2 = UserInfo(pk3, "SmnTin", lastSeen, false)

        val friend1 = Friend(friendInfo1, true,
            Random.nextLong(0, 100000),
            Random.nextLong(0, 100000))
        val friend2 = Friend(friendInfo2, false,
            Random.nextLong(0, 100000),
            Random.nextLong(0, 100000))

        val me = Account(userInfo, pr, mutableMapOf(
            byteArrayToString(friendInfo1.pubKey) to friend1,
            byteArrayToString(friendInfo2.pubKey) to friend2)
        )

        val dataStorage : UserDataStorage = ProtoUserDataStorageImpl(testUserInfoPathProto)

        dataStorage.saveMyData(me)
        println(me)
        println(dataStorage.loadMyData())
        assertEquals(me, dataStorage.loadMyData())
    }

    @RepeatedTest(90)
    fun `multiple account data to JSON save test`(testInfo : TestInfo) {
        val dataStorage : UserDataStorage = JSONUserDataStorageImpl(testUserInfoPathJSON)

        val me = createAccount()
        dataStorage.saveMyData(me)
        assertEquals(me, dataStorage.loadMyData())

        println("Test display name: ${testInfo.displayName}")
    }

    @RepeatedTest(90)
    fun `multiple account data to Proto save test`(testInfo : TestInfo) {
        val dataStorage : UserDataStorage = ProtoUserDataStorageImpl(testUserInfoPathProto)

        val me = createAccount()
        dataStorage.saveMyData(me)
        assertEquals(me, dataStorage.loadMyData())

        println("Test display name: ${testInfo.displayName}")
    }

    private fun createAccount() : Account {
        val pk = Random.nextBytes(32)
        val pr = Random.nextBytes(32)

        val lastSeen = System.currentTimeMillis()
        val userInfo = UserInfo(pk, getRandomString(9), lastSeen, Random.nextBoolean())

        val me = Account(userInfo, pr, mutableMapOf())

        for (i in 0 until Random.nextInt(200)) {
            val friend = createFriend()
            me.friends[byteArrayToString(friend.userInfo.pubKey)] = friend
        }

        return me
    }
}