package ru.emkn.p2beer.app.client.chat

/**
 * Inserts [message] in [node].
 *
 * It's guaranteed that [node] isn't full.
 * If [node] is leaf then adds [pointerToMessage] to [node].pointersToMessages.
 * Otherwise determines appropriate child and calls function from this child.
 */
fun insertMessageInNonFull(bTree: BTree, node: Node, message: Message, pointerToMessage: Long) {
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    var position = messagesInThisNode.binarySearch(message, MessageComparator)
    // index of first message that is newer
    position = -position - 1

    if (node.isLeaf) {
        movePointers(bTree.fileWithIndex, node.positionInFile + position * Long.SIZE_BYTES,
                node.pointersToMessages.subList(position, node.pointersToMessages.size))

        writeLong(bTree.fileWithIndex, node.positionInFile + position * Long.SIZE_BYTES, pointerToMessage)
    }
    else {
        val childNode = getNode(bTree, node.pointersToChildren[position])

        if (childNode.pointersToMessages.size == 2 * bTree.t - 1) { // childNode is full -> split it into 2
            val updatedNode = splitChild(bTree, node, childNode, position)
            insertMessageInNonFull(bTree, updatedNode, message, pointerToMessage)
        }
        else
            insertMessageInNonFull(bTree, childNode, message, pointerToMessage)
    }
}

/**
 * Splits full [child] into 2 nodes: first half of [child] and newNode.
 *
 * Leaves first (t - 1) messages and t children in [child].
 * Adds last (t - 1) messages and t children into newNode.
 * Inserts pointerToMiddleMessage and newNode into [node].
 *
 * @return updated [node] - with one more message and one more child.
 */
fun splitChild(bTree: BTree, node: Node, child: Node, id: Int): Node {

    val messagesOfNewNode = child.pointersToMessages.subList(bTree.t, child.pointersToMessages.size)

    val childrenOfNewNode = if (child.isLeaf) mutableListOf<Long>()
        else child.pointersToChildren.subList(bTree.t, child.pointersToChildren.size)
    val newNode = Node(bTree.fileWithIndex.length(),
            messagesOfNewNode,
            child.isLeaf,
            childrenOfNewNode,
            bTree)

    movePointers(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES,
            // pointerToMiddleMessage goes on id position ->
            node.pointersToMessages.subList(id, node.pointersToMessages.size))
    // -> we should move all messages starting from id

    val pointerToMiddleMessage = child.pointersToMessages[bTree.t - 1]

    node.pointersToMessages.add(id, pointerToMiddleMessage)

    writeLong(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES,
            pointerToMiddleMessage)

    val positionOfFirstChild = node.positionInFile + (2 * bTree.t - 1) * Long.SIZE_BYTES + 1

    movePointers(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES,
            // newNode goes on (id + 1) position ->
            node.pointersToChildren.subList(id + 1, node.pointersToChildren.size))
    // -> we should move all children starting from (id + 1)

    node.pointersToChildren.add(id + 1, newNode.positionInFile)

    writeLong(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES, newNode.positionInFile)

    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t - 1) * Long.SIZE_BYTES, bTree.t)
    // delete snd half of messages from child (they went to newNode)
    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t * 3 - 1) * Long.SIZE_BYTES + 1, bTree.t)
    // delete snd half of children from child (they went to newNode)

    return node
}

fun findKNextMessages(bTree: BTree, node: Node, message: Message, k: Int, amountLeft: Int): List<Message> {
    if (amountLeft <= 0)
        return listOf()
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    val messages = mutableListOf<Message>()

    if (node.isLeaf) {
        var position = messagesInThisNode.binarySearch(message, MessageComparator)
        if (position < 0)
            position = -position - 1

        return messagesInThisNode.subList(position,
                position + minOf(messagesInThisNode.size - position, amountLeft))
    }
    else {
        var indexOfGoodChild = 0

        if (amountLeft == k) {
            var position = messagesInThisNode.binarySearch(message, MessageComparator)
            // index of first message that is newer
            if (position >= 0)
                messages.add(message)
            else {
                position = -position - 1

                val childNode = getNode(bTree, node.pointersToChildren[position])

                messages += findKNextMessages(bTree, childNode, message, k, amountLeft)

                if (messages.size < k && position < messagesInThisNode.size)
                    messages.add(messagesInThisNode[position])
            }
            if (messages.size == k)
                return messages
            else
                indexOfGoodChild = position + 1
        }
        for (index in indexOfGoodChild until node.pointersToChildren.size) {
            val pointer = node.pointersToChildren[index]
            val child = getNode(bTree, pointer)

            messages += findKNextMessages(bTree, child, message, k, amountLeft - messages.size)

            if (messages.size < amountLeft && index != node.pointersToChildren.lastIndex)
                messages.add(messagesInThisNode[index])

            if (messages.size == amountLeft)
                break
        }
    }
    return messages
}

fun findKPreviousMessages(bTree: BTree, node: Node, message: Message, k: Int, amountLeft: Int): List<Message> {
    if (amountLeft <= 0)
        return listOf()
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    val messages = mutableListOf<Message>()

    if (node.isLeaf) {
        var position = messagesInThisNode.binarySearch(message, MessageComparator)
        if (position < 0)
            position = -position - 2

        while (position >= 0 && messages.size < amountLeft) {
            messages.add(messagesInThisNode[position])
            position--
        }
    }
    else {
        var indexOfGoodMessage = node.pointersToMessages.size - 1

        if (amountLeft == k) {
            var position = messagesInThisNode.binarySearch(message, MessageComparator)
            // index of first message that is newer
            if (position < 0) {
                position = -position - 1

                val childNode = getNode(bTree, node.pointersToChildren[position])

                messages += findKPreviousMessages(bTree, childNode, message, k, amountLeft)

                position--
            }
            if (messages.size == k)
                return messages
            else
                indexOfGoodMessage = position
        }
        else {
            val lastChild = getNode(bTree, node.pointersToChildren.last())
            messages += findKPreviousMessages(bTree, lastChild, message, k, amountLeft)
            if (messages.size == amountLeft)
                return messages
        }

        for (index in indexOfGoodMessage downTo 0) {
            messages.add(messagesInThisNode[index])
            if (messages.size == amountLeft)
                break

            val pointer = node.pointersToChildren[index]
            val child = getNode(bTree, pointer)

            messages += findKPreviousMessages(bTree, child, message, k, amountLeft - messages.size)

            if (messages.size == amountLeft)
                break
        }
    }
    return messages
}

fun findLastMessage(bTree: BTree, node: Node): Message {
    if (node.isLeaf)
        return readMessage(bTree.fileWithMessages, node.pointersToMessages.last()) // last message in this node

    val lastChild = getNode(bTree, node.pointersToChildren.last())
    return findLastMessage(bTree, lastChild)
}