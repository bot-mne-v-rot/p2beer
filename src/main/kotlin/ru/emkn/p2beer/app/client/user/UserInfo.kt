package ru.emkn.p2beer.app.client.user

import com.google.gson.annotations.SerializedName

data class UserInfo (
    @SerializedName("pubKey") var pubKey: ByteArray,
    @SerializedName("userName") val userName: String,
    @SerializedName("lastName") var lastSeen: Long,
    @SerializedName("onlineStatus") var onlineStatus: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserInfo

        if (!pubKey.contentEquals(other.pubKey)) return false
        if (userName != other.userName) return false
        if (lastSeen != other.lastSeen) return false
        if (onlineStatus != other.onlineStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pubKey.contentHashCode()
        result = 31 * result + userName.hashCode()
        result = 31 * result + lastSeen.hashCode()
        result = 31 * result + onlineStatus.hashCode()
        return result
    }
}
