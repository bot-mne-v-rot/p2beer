package ru.emkn.p2beer.app.client.user

import java.nio.file.Path

abstract class PrivateKey {
    var privateKey : PrivateKey? = null

    abstract  fun generatePrivateKey () : PrivateKey

    abstract fun loadPrivateKey ( filePath: Path ) : PrivateKey

    abstract fun saveKey ( prv: PrivateKey )
}