package ru.emkn.p2beer.app.client.user

data class Account (
    val userInfo: UserInfo,
    val privateKey: PrivateKey,
    val friends: Set<Friend>
)

data class Friend (
    val userInfo: UserInfo
)