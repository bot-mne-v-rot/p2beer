package ru.emkn.p2beer.app.p2bLib

import ru.emkn.p2beer.app.client.user.*

class FriendsManagerDefaultImpl (
    override var allFriendsConnected: Boolean
) : FriendsManager {
    override fun addFriend(userId: PublicKey): Friend {
        TODO("Not yet implemented")
    }

    override fun removeFriend(userId: PublicKey) {
        TODO("Not yet implemented")
    }

    override fun getConnectionTo(friend: Friend): FriendConnection {
        TODO("Not yet implemented")
    }

    override fun connectAll(friends: Set<Friend>) {
        TODO("Not yet implemented")
    }

}