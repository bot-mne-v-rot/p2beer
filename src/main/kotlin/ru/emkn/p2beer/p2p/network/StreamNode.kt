package ru.emkn.p2beer.p2p.network

typealias Buffer = UByteArray

/**
 * A base abstraction for the protocol.
 * Stream nodes form a tree.
 * A stream is opened per each connection and
 * extended with decorator streams in a chain
 * of extensions.
 */
interface StreamNode {
    var parent : StreamNode?

    /**
     * The message should be sent via transport to the remote node.
     * Invocation flow goes from children to parent.
     */
    fun send(message : Buffer)

    /**
     * The message was received by the transport and should be propagated
     * to the children.
     * Invocation flow goes from parent to children.
     */
    fun receive(message : Buffer)

    /**
     * The method is firstly called on the root node and only when the
     * connection is opened by our side.
     *
     * Should initialize current node by sending handshake messages
     * via parent nodes which should be initialized by the time
     * and then call the method on children.
     *
     * Handshake process may not be passed to children right when
     * the performHandshake method is called on the current node itself.
     * The handshake may be split into several stages and may be passed
     * only after full completion that may require communication with the
     * remote instance of the stream node. It can be implemented via
     * State pattern, for instance.
     */
    fun performHandshake()

    /**
     * The same as [performHandshake] but in reverse order.
     * It's is still firstly called on the root node
     * but the node should firstly let the children nodes to
     * close and only after that close itself.
     */
    fun performClosure()

    /**
     * The method just to pass the node's wish to close the connection.
     */
    fun close() {
        if (parent == null)
            performClosure()
        parent?.close()
    }

    /**
     * True if the current stream node was successfully opened
     * regardless the invocation side.
     */
    val opened : Boolean

    /**
     * True if the current stream node was successfully closed
     * regardless the invocation side.
     */
    val closed : Boolean
}

/**
 * Example implementation of a node with no children.
 */
open class StreamLeafNode : StreamNode {
    override var parent: StreamNode? = null

    override var opened: Boolean = false
         protected set

    override var closed: Boolean = false
        protected set

    override fun performHandshake() {
        // Handshake
        opened = true
    }

    override fun performClosure() {
        // Closure
        closed = true
    }

    override fun send(message: Buffer) {
        // Message processing
        parent?.send(message)
    }

    override fun receive(message: Buffer) {
        // Message processing
    }
}

/**
 * Example implementation of a node with a single child.
 */
open class StreamListNode : StreamLeafNode() {

    // Automatically inserts parent
    var child: StreamNode? = null
        set(value) {
            value?.parent = this
            field = value
        }

    override fun performHandshake() {
        // Handshake
        opened = true
        child?.performHandshake()
    }

    override fun performClosure() {
        child?.performClosure()
        // Closure
        closed = true
    }

    override fun receive(message: Buffer) {
        // Message processing
        parent?.receive(message)
    }
}