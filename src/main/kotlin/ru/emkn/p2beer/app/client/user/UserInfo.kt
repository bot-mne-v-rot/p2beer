package ru.emkn.p2beer.app.client.user

data class UserInfo (
    var pubKey: PublicKey,
    val userName: String,
    var lastSeen: Long,
    var onlineStatus: Boolean
)