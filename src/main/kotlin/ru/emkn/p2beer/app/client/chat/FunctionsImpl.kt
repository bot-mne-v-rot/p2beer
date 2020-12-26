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
    position = -position - 1 // index of first message that is newer

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
    // pointerToMiddleMessage takes id position,
    // so we should move all messages starting from id
    movePointers(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES,
            node.pointersToMessages.subList(id, node.pointersToMessages.size))

    val pointerToMiddleMessage = child.pointersToMessages[bTree.t - 1]
    node.pointersToMessages.add(id, pointerToMiddleMessage)

    writeLong(bTree.fileWithIndex, node.positionInFile + id * Long.SIZE_BYTES,
            pointerToMiddleMessage) // add pointerToMiddleMessage to parent node

    val positionOfFirstChild = node.positionInFile + (2 * bTree.t - 1) * Long.SIZE_BYTES + 1
    // newNode takes (id + 1) position,
    // so we should move all children starting from (id + 1)
    movePointers(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES,
            node.pointersToChildren.subList(id + 1, node.pointersToChildren.size))

    node.pointersToChildren.add(id + 1, newNode.positionInFile)
    writeLong(bTree.fileWithIndex, positionOfFirstChild + (id + 1) * Long.SIZE_BYTES, newNode.positionInFile)

    // delete second half of messages from child (they went to newNode)
    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t - 1) * Long.SIZE_BYTES, bTree.t)
    // delete second half of children from child (they went to newNode)
    deletePointers(bTree.fileWithIndex, child.positionInFile + (bTree.t * 3 - 1) * Long.SIZE_BYTES + 1, bTree.t)
    return node
}

/**
 * Implementation of getKNextMessages.
 *
 * Search for [message] and then starts to take messages right after it.
 * [k] - number of messages that we should take.
 */
fun findKNextMessages(bTree: BTree, node: Node, message: Message, k: Int): List<Message> {
    if (k <= 0)
        return listOf()
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    val messages = mutableListOf<Message>()

    var position = messagesInThisNode.binarySearch(message, MessageComparator)
    if (position < 0) position = -position - 1 else position++

    for (i in position until messagesInThisNode.size) {
        if (!node.isLeaf) {
            val child = getNode(bTree, node.pointersToChildren[i])
            messages += findKNextMessages(bTree, child, message, k - messages.size)
        }
        if (messages.size < k) messages.add(messagesInThisNode[i]) else break
    }
    if (!node.isLeaf && messages.size < k) {
        val lastChild = getNode(bTree, node.pointersToChildren.last())
        messages += findKNextMessages(bTree, lastChild, message, k - messages.size)
    }
    return messages
}

/**
 * Implementation of getKPreviousMessages.
 *
 * Search for [message] and then starts to take messages right before it.
 * [k] - number of messages that we should take.
 */
fun findKPreviousMessages(bTree: BTree, node: Node, message: Message, k: Int): List<Message> {
    if (k <= 0)
        return listOf()
    val messagesInThisNode = getMessages(node, bTree.fileWithMessages)

    val messages = mutableListOf<Message>()

    var position = messagesInThisNode.binarySearch(message, MessageComparator)
    if (position < 0) position = -position - 2 else position--

    if (!node.isLeaf) {
        val child = getNode(bTree, node.pointersToChildren[position + 1])
        messages += findKPreviousMessages(bTree, child, message, k)
    }

    for (i in position downTo 0) {
        if (messages.size < k) messages.add(messagesInThisNode[i]) else break
        if (!node.isLeaf) {
            val child = getNode(bTree, node.pointersToChildren[i])
            messages += findKPreviousMessages(bTree, child, message, k - messages.size)
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
