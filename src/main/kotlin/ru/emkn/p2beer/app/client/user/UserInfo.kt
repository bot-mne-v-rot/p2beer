package ru.emkn.p2beer.app.client.user

import ru.emkn.p2beer.app.client.chat.*
import java.security.*

data class UserInfo (
    val pubKey: PublicKey,
    val userName: String
)