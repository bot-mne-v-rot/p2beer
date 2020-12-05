package ru.emkn.p2beer.p2p.network

data class ProtocolVersion(val major: UShort, val minor: UShort, val revision: UShort) {
    operator fun compareTo(other: ProtocolVersion): Int {
        return when {
            major != other.major -> major.toInt() - other.major.toInt()
            minor != other.minor -> minor.toInt() - other.minor.toInt()
            else -> revision.toInt() - other.revision.toInt()
        }
    }
}

data class ProtocolDescriptor(
    val name: String,
    val version: ProtocolVersion,
    var leastSupportedVersion: ProtocolVersion
) {

    constructor(name: String, version: ProtocolVersion) : this(name, version, version)

    infix fun compatibleWith(current: ProtocolDescriptor): Boolean {
        return name == current.name &&
                current.leastSupportedVersion <= version &&
                version <= current.version
    }
}

abstract class ProtocolRouterExtension : ExtensionLeafNode() {
    /**
     * Even though protocols can be removed on the fly, the actual structure
     * of existing streams will not be changed.
     */
    abstract val protocols: MutableMap<ProtocolDescriptor, ExtensionNode>

    abstract override suspend fun extendStream(node: StreamListNode)
}

abstract class ProtocolRouterStream : StreamLeafNode() {
    /**
     * @throws NoSuchElementException if protocol is not found or this version is not supported
     */
    abstract override suspend fun receive(message: Buffer)
}