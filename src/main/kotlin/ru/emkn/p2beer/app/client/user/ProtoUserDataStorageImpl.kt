package ru.emkn.p2beer.app.client.user

import com.google.protobuf.ByteString
import ru.emkn.p2beer.app.UserDataProto
import ru.emkn.p2beer.app.client.util.userInfoPathProto
import java.io.File
import java.io.RandomAccessFile

class ProtoUserDataStorageImpl : UserDataStorage {
    override fun saveMyData(me: Account) {
        val file = RandomAccessFile(File(userInfoPathProto), "rw")
        file.write(serializeAccount(me))
    }

    override fun loadMyData(): Account {
        val file = RandomAccessFile(File(userInfoPathProto), "r")
        val bytes = ByteArray(file.length().toInt())
        file.readFully(bytes)
        return deserializeAccount(bytes)
    }

    private fun serializeAccount(me: Account) =
        UserDataProto.Account
            .newBuilder()
            .setPrivateKey(ByteString.copyFrom(me.privateKey))
            .setUserInfo(serializeUserInfo(me.userInfo))
            .putAllFriends(me.friends.mapValues { serializeFriend(it.value) })
            .build()
            .toByteArray()

    private  fun deserializeAccount(bytes: ByteArray) : Account {
        val proto =
            UserDataProto.Account
                .parseFrom(bytes)

        return Account(
            deserializeUserInfo(proto.userInfo),
            proto.privateKey.toByteArray(),
            proto.friendsMap.mapValues { deserializeFriend(it.value) }
        )
    }

    private fun serializeUserInfo(userInfo: UserInfo) =
        UserDataProto.UserInfo
            .newBuilder()
            .setUserName(userInfo.userName)
            .setLastSeen(userInfo.lastSeen)
            .setPubKey(ByteString.copyFrom(userInfo.pubKey))
            .setOnlineStatus(userInfo.onlineStatus)
            .build()

    private fun deserializeUserInfo(proto: UserDataProto.UserInfo) =
        UserInfo(
            proto.pubKey.toByteArray(),
            proto.userName,
            proto.lastSeen,
            proto.onlineStatus
        )

    private fun serializeFriend(friend: Friend) =
        UserDataProto.Friend
            .newBuilder()
            .setUserInfo(serializeUserInfo(friend.userInfo))
            .setIsConnection(friend.isConnection)
            .setMessagesCount(friend.messagesCount)
            .setLastMessageTimeStamp(friend.lastMessageTimeStamp)
            .build()

    private fun deserializeFriend(proto: UserDataProto.Friend) =
        Friend(
            deserializeUserInfo(proto.userInfo),
            proto.isConnection,
            proto.messagesCount,
            proto.lastMessageTimeStamp
        )
}