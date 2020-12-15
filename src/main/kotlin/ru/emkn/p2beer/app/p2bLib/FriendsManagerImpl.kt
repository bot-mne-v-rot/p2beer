package ru.emkn.p2beer.app.p2bLib

import ru.emkn.p2beer.app.client.user.*

class FriendsManagerImpl (
    override var allFriendsConnected: Boolean
) : FriendsManager {
    override fun addFriend(userId: ByteArray): Friend {
        TODO("Not yet implemented")
    }

    override fun removeFriend(userId: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun getConnectionTo(friend: Friend): FriendConnection {
        TODO("Not yet implemented")
    }

    override fun connectAll(friends: Set<Friend>) {
        TODO("Not yet implemented")
    }

}