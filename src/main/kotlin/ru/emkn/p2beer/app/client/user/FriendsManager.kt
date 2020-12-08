package ru.emkn.p2beer.app.client.user

interface FriendsManager {
    var allFriendsConnected : Boolean

    fun addFriend (userId : PublicKey) : Friend

    fun removeFriend (userId : PublicKey)

    fun getConnectionTo (friend : Friend) : FriendConnection

    fun connectAll (friends : Set<Friend>)
}