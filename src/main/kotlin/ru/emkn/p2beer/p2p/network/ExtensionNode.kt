package ru.emkn.p2beer.p2p.network

/**
 * One of the base abstraction of the protocol.
 * While stream is opened per each connection per each
 * transport, extensions are constructed only once and
 * then are connected to transport manager.
 *
 * Their work is to extend streams with specific to this extension
 * instances of [StreamNode].
 *
 * Typically [ExtensionNode] and according [StreamNode] are implemented in
 * pair. Complex logic involving communication with several nodes should be
 * implemented in [ExtensionNode].
 *
 * @see StreamNode
 */
interface ExtensionNode {
    var parent: ExtensionNode?

    /**
     * Can only extend successors of [StreamListNode].
     * If your extension implements multiple branching
     * dummy streams should be passed to the successive
     * nodes.
     *
     * @see ProtocolRouter
     */
    fun extendStream(node: StreamListNode)
}

/**
 * @see StreamLeafNode
 */
open class ExtensionLeafNode : ExtensionNode {
    override var parent: ExtensionNode? = null

    override fun extendStream(node: StreamListNode) {
        // Example implementation.
        // Can be overridden.
        node.child = StreamLeafNode()
    }
}

/**
 * @see StreamListNode
 */
open class ExtensionListNode : ExtensionLeafNode() {
    var child: ExtensionNode? = null
        set(value) {
            value?.parent = this
            field = value
        }

    override fun extendStream(node: StreamListNode) {
        // Example implementation.
        // Can be overridden.
        val successor = StreamListNode()
        node.child = successor
        child?.extendStream(successor)
    }
}