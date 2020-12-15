package ru.emkn.p2beer.app.client.user

interface FriendsManager {
    var allFriendsConnected : Boolean

    fun addFriend (userId : ByteArray) : Friend

    fun removeFriend (userId : ByteArray)

    fun getConnectionTo (friend : Friend) : FriendConnection

    fun connectAll (friends : Set<Friend>)
}