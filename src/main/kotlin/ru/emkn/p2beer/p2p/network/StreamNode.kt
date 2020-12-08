package ru.emkn.p2beer.p2p.network

import ru.emkn.p2beer.p2p.NodeId
import ru.emkn.p2beer.p2p.Buffer

/**
 * One of the base abstractions for the protocol.
 * Stream nodes form a tree.
 * A stream is opened per each connection and
 * extended with decorator streams in a chain
 * of extensions.
 *
 * @see ExtensionNode
 */
interface StreamNode {
    var parent: StreamNode?

    /**
     * The message should be sent via transport to the remote node.
     * Invocation flow goes from children to parent.
     *
     * @throws IllegalStateException
     */
    suspend fun send(message: Buffer)

    /**
     * The message was received by the transport and should be propagated
     * to the children.
     * Invocation flow goes from parent to children.
     *
     * @throws IllegalStateException
     */
    suspend fun receive(message: Buffer)

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
     *
     * @throws HandshakeFailedException
     * @throws IllegalStateException
     */
    suspend fun performHandshake()

    /**
     * The same as [performHandshake] but in reverse order.
     * It's is still firstly called on the root node
     * but the node should firstly let the children nodes to
     * close and only after that close itself.
     *
     * @throws ClosureFailedException
     * @throws IllegalStateException
     */
    suspend fun performClosure()

    /**
     * The method just to pass the node's wish to close the connection.
     */
    suspend fun close() {
        if (parent == null)
            performClosure()
        parent?.close()
    }

    /**
     * True if the current stream node was successfully opened
     * regardless the invocation side.
     *
     * False if the current stream node was successfully closed
     * or wasn't yet initialized.
     */
    val opened: Boolean

    /**
     * Transport descriptor inherited from the [Transport]
     *
     * DON'T ACCESS BEFORE PARENT IS ATTACHED
     */
    val transport: TransportDescriptor

    /**
     * Our node id. It's one for the whole application
     * but is put here for convenience.
     *
     * DON'T ACCESS BEFORE PARENT IS ATTACHED
     */
    val thisNodeId: NodeId

    /**
     * Remote node's NodeId associated with the Stream
     *
     * DON'T ACCESS BEFORE PARENT IS ATTACHED
     */
    val remoteNodeId: NodeId

    /**
     * Endpoint from which we communicate
     *
     * DON'T ACCESS BEFORE PARENT IS ATTACHED
     */
    val thisEndpoint: Endpoint

    /**
     * Endpoint to which we communicate
     *
     * DON'T ACCESS BEFORE PARENT IS ATTACHED
     */
    val remoteEndpoint: Endpoint
}

suspend fun StreamNode.receiveString(message: String) {
    receive(message.encodeToByteArray())
}

suspend fun StreamNode.sendString(message: String) {
    send(message.encodeToByteArray())
}

/**
 * Example implementation of a node with no children.
 */
open class StreamLeafNode : StreamNode {
    override var parent: StreamNode? = null

    /**
     * Some nodes can state that they have no handshake process
     * and thus are opened by default. We can't simply have the
     * variable set true during [performHandshake] because
     * absence of the handshake messages results in the non-opened
     * remote node.
     */
    override var opened: Boolean = true
        protected set

    override suspend fun performHandshake() {
        // Handshake

        // Normally:
        // opened = true
    }

    override suspend fun performClosure() {
        // Closure
        opened = false
    }

    override suspend fun send(message: Buffer) {
        // Message processing
        parent?.send(message)
    }

    override suspend fun receive(message: Buffer) {
        // Message processing
    }

    override val thisNodeId: NodeId by lazy {
        parent!!.thisNodeId
    }

    override val remoteNodeId: NodeId by lazy {
        parent!!.remoteNodeId
    }

    override val transport: TransportDescriptor by lazy {
        parent!!.transport
    }

    override val thisEndpoint: Endpoint by lazy {
        parent!!.thisEndpoint
    }

    override val remoteEndpoint: Endpoint by lazy {
        parent!!.remoteEndpoint
    }
}

/**
 * Example implementation of a node with a single child.
 */
open class StreamListNode : StreamLeafNode() {

    // Automatically inserts parent
    open var child: StreamNode? = null
        set(value) {
            value?.parent = this
            field = value
        }

    override suspend fun performHandshake() {
        // Handshake

        // Normally:
        // opened = true
        child?.performHandshake()
    }

    override suspend fun performClosure() {
        child?.performClosure()
        // Closure
        opened = false
    }

    override suspend fun receive(message: Buffer) {
        // Message processing
        child?.receive(message)
    }
}

class HandshakeFailedException : Exception {
    constructor() : super("Handshake failed.")
    constructor(message: String) : super("Handshake failed due to:\n $message")
}

class ClosureFailedException : Exception {
    constructor() : super("Closure failed.")
    constructor(message: String) : super("Closure failed due to:\n $message")
}