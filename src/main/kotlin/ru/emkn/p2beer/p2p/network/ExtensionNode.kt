package ru.emkn.p2beer.p2p.network

import kotlinx.coroutines.CoroutineScope

/**
 * One of the base abstractions of the protocol.
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
    /**
     * Null by default. Usually set in the root extension by the
     * [TransportManager].
     */
    var backgroundScope: CoroutineScope?

    var parent: ExtensionNode?

    /**
     * Called by the [TransportManager] when the latter is extended.
     * Use it with [backgroundScope] to create background activities.
     */
    suspend fun init() = Unit

    /**
     * Can only extend successors of [StreamListNode].
     * If your extension implements multiple branching
     * dummy streams should be passed to the successive
     * nodes.
     *
     * @see ProtocolRouterExtension
     */
    suspend fun extendStream(node: StreamListNode)
}

/**
 * @see StreamLeafNode
 */
open class ExtensionLeafNode : ExtensionNode {
    override var backgroundScope: CoroutineScope? = null
        get() {
            // Propagate from the parent to the children
            field = field ?: parent?.backgroundScope
            return field
        }

    override var parent: ExtensionNode? = null

    override suspend fun extendStream(node: StreamListNode) {
        // Example implementation.
        // Can be overridden.
        node.child = StreamLeafNode()
    }
}

/**
 * @see StreamListNode
 */
open class ExtensionListNode : ExtensionLeafNode() {
    /**
     * If the child is hot-swapped the [init] method
     * should be called explicitly
     */
    var child: ExtensionNode? = null
        set(value) {
            value?.parent = this
            field = value
        }

    override suspend fun extendStream(node: StreamListNode) {
        // Example implementation.
        // Can be overridden.
        val successor = StreamListNode()
        node.child = successor
        child?.extendStream(successor)
    }
}