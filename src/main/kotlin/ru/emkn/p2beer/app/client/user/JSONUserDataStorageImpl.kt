package ru.emkn.p2beer.app.client.user

import com.google.gson.*
import ru.emkn.p2beer.app.client.util.*
import java.io.File
import java.io.RandomAccessFile
import java.security.PublicKey

//TODO: Create saving to binary
// file and loading from binary file

data class LastChattedFriend(val publicKey: ByteArray,
                             val messagesCount: Long,
                             val lastMessageTimeStamp: Long) {
}

fun saveToFile(pathToFile: String, lastChattedFriend: LastChattedFriend) {
    val file = RandomAccessFile(File(pathToFile), "w")
    file.write(lastChattedFriend.publicKey)
    file.writeLong(lastChattedFriend.messagesCount)
    file.writeLong(lastChattedFriend.lastMessageTimeStamp)
}

fun loadFromFile(pathToFile: String): LastChattedFriend {
    val file = RandomAccessFile(File(pathToFile), "r")
    val buffer = ByteArray(32)
    file.read(buffer)
    return LastChattedFriend(buffer, messagesCount = file.readLong(), file.readLong())
}

class JSONUserDataStorageImpl : UserDataStorage {
    override fun saveMyData(me: Account) {
        val jsonString = Gson().toJson(me)

        saveDataJSON(jsonString)
    }

    override fun loadMyData() : Account {
        val jsonString = File(userInfoPath).readText()

        return Gson().fromJson(jsonString, Account::class.java)
    }

    private fun saveDataJSON (data: String) {

        /**
         * Make JSON look pretty using GSON
         */

        val gson = GsonBuilder().setPrettyPrinting().create()
        val je: JsonElement = JsonParser.parseString(data)
        val prettyJsonString = gson.toJson(je)

        /**
         * Write our Object with all words and
         * information about them to a JSON file
         */

        File(userInfoPath).writeText(prettyJsonString)
    }
}
