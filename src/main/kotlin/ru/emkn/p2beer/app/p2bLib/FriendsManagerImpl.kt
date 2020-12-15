package ru.emkn.p2beer.app.p2bLib

import ru.emkn.p2beer.app.client.user.*

class FriendsManagerImpl  : FriendsManager {

    override var allFriendsConnected: Boolean = false

    override fun addFriend(userId: ByteArray): Friend {
        TODO("Not yet implemented")
    }

    override fun removeFriend(userId: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun getConnectionTo(friend: Friend): FriendConnection {
        return FriendConnection()
    }

    override fun connectAll(friends: Set<Friend>) {
        allFriendsConnected = true
        TODO("Not yet implemented")
    }

}