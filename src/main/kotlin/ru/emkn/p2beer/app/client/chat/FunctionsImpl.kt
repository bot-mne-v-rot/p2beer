package ru.emkn.p2beer.app.client.chat


/*
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

/**
 * Recursively searches for last message.
 */
fun findLastMessage(bTree: BTree, node: Node): Message {
    if (node.isLeaf)
        return readMessage(bTree.fileWithMessages, node.pointersToMessages.last()) // last message in this node

    val lastChild = getNode(bTree, node.pointersToChildren.last())
    return findLastMessage(bTree, lastChild)
}
*/