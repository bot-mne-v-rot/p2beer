package ru.emkn.p2beer.app.client.user

import kotlin.random.Random

data class PublicKey (
        val key : String = Random.nextLong(10000000, 99999999).toString()
)

