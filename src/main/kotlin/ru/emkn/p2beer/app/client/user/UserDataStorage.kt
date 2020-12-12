package ru.emkn.p2beer.app.client.user

interface UserDataStorage {
    fun saveMyData (me: Account)

    fun loadMyData () : Account
}