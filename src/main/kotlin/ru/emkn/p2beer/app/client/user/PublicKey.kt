package ru.emkn.p2beer.app.client.user

import java.nio.file.Path

abstract class PublicKey {
    var publicKey : PublicKey? = null

    abstract fun generatePublicKey () : PublicKey

    abstract fun loadPublicKey ( filePath: Path ) : PublicKey

    abstract fun saveKey ( pub: PublicKey )
}

