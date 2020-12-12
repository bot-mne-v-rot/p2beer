package ru.emkn.p2beer.app.client.user

import com.google.gson.annotations.SerializedName

data class Account (
    @SerializedName("userInfo") val userInfo: UserInfo,
    @SerializedName("privateKey") val privateKey: PrivateKey,
    @SerializedName("friends") val friends: Set<Friend>
)

data class Friend (
    @SerializedName("userInfo") val userInfo: UserInfo,
    @SerializedName("isConnected") var isConnection: Boolean
)
