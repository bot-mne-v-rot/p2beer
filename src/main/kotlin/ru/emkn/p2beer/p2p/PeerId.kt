package ru.emkn.p2beer.p2p

// For byte bitwise operations only
import kotlin.random.Random
import kotlin.random.nextUBytes

const val PeerId_sizeInBytes = 32

/**
 * Represents long number of [PeerId_sizeInBytes].
 * The most significant byte is the first one (its index is 0).
 * The default bit order in each individual byte is preserved.
 */
data class PeerId(val data: UByteArray = UByteArray(PeerId_sizeInBytes)) {
    init {
        assert(data.size == PeerId_sizeInBytes)
    }

    infix fun xor(other: PeerId) =
        PeerId((data zip other.data).map { (a, b) ->
            a xor b
        }.toUByteArray())

    infix fun and(other: PeerId) =
        PeerId((data zip other.data).map { (a, b) ->
            a and b
        }.toUByteArray())

    infix fun or(other: PeerId) =
        PeerId((data zip other.data).map { (a, b) ->
            a or b
        }.toUByteArray())

    fun inv() = PeerId(data.map { it.inv() }.toUByteArray())

    operator fun get(index: Int) = data[index]

    operator fun set(index: Int, value: UByte) {
        data[index] = value
    }

    /**
     * @return if bit at pos is set
     */
    fun bitAt(pos: Int): Boolean {
        val bytePos = pos / UByte.SIZE_BITS
        val bitPos = pos % UByte.SIZE_BITS
        val byte = data[bytePos]
        return ((byte.toInt() shr (UByte.SIZE_BITS - 1 - bitPos)) and 1) > 0
    }

    /**
     * Compares two other NodeId's relatively to this
     * @return negative value if the first is less than the second
     * @return positive value if the first is greater than the second
     * @return zero if the first equals to the second
     */
    fun xorCmp(a: PeerId, b: PeerId): Int {
        val xorAB = (a xor b)
        val ind = xorAB.data.indexOfFirst { it > 0u }

        if (ind == -1)
            return 0

        // It is not the clearest code but it is the only one that current
        // version of Idea could parse without making my laptop overheat
        return (a.data[ind] xor data[ind]).toInt() -
                (b.data[ind] xor data[ind]).toInt()
    }

    /**
     * Longest common prefix of two NodeId starting from
     * the most significant bit (the MSB of the first byte).
     *
     * Can be considered as the 0-indexed position (from the MSB side)
     * of the MSB in xor of two ids. It is the actual implementation.
     *
     * @return length of the longest common prefix in bits. If
     * ids are equal, size of id in bits is returned.
     */
    infix fun lcp(other: PeerId) =
        (this xor other).countLeadingZeroBits()

    /**
     * @return the most significant set bit of the id if any.
     * Actually, the length of the longest prefix of zeroes.
     * @return [sizeInBits] if all bits are zeroes
     */
    fun countLeadingZeroBits(): Int {
        val bytePos = data.indexOfFirst { it > 0u }
        if (bytePos == -1)
            return sizeInBits

        val byte = data[bytePos]
        return bytePos * Byte.SIZE_BITS + byte.countLeadingZeroBits()
    }

    companion object {
        const val sizeInBytes = PeerId_sizeInBytes
        const val sizeInBits = PeerId_sizeInBytes * UByte.SIZE_BITS

        fun zeroes(): PeerId {
            val nodeId = PeerId()
            nodeId.data.fill(0u)
            return nodeId
        }

        fun max(): PeerId {
            val nodeId = PeerId()
            nodeId.data.fill(UByte.MAX_VALUE)
            return nodeId
        }

        fun random() =
            PeerId(Random.nextUBytes(PeerId_sizeInBytes))
    }

    /**
     * Automatically generated method.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerId

        if (!data.contentEquals(other.data)) return false

        return true
    }

    /**
     * Automatically generated method.
     */
    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    override fun toString(): String =
        data.joinToString(".") { it.toString(16).toUpperCase().padStart(2, '0') }

    fun toStringBin(): String =
        data.joinToString(".") { it.toString(2).padStart(UByte.SIZE_BITS, '0') }
}