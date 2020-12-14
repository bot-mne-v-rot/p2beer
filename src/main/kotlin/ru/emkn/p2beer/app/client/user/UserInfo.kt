package ru.emkn.p2beer.app.client.user

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

data class UserInfo (
    @SerializedName("pubKey") var pubKey: PublicKey,
    @SerializedName("userName") val userName: String,
    @SerializedName("lastName") var lastSeen: Long,
    @SerializedName("onlineStatus") var onlineStatus: Boolean
)