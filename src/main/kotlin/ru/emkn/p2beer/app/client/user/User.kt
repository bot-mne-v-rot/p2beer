package ru.emkn.p2beer.app.client.user

import com.google.gson.annotations.SerializedName

data class Account(
    @SerializedName("userInfo") val userInfo: UserInfo,
    @SerializedName("privateKey") val privateKey: ByteArray,
    @SerializedName("friends") val friends: Set<Friend>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (userInfo != other.userInfo) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (friends != other.friends) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userInfo.hashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + friends.hashCode()
        return result
    }
}

data class Friend (
    @SerializedName("userInfo") val userInfo: UserInfo,
    @SerializedName("isConnected") var isConnection: Boolean
)
