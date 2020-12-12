package ru.emkn.p2beer.app.client.user

import com.google.gson.*
import ru.emkn.p2beer.app.client.util.*
import java.io.File

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