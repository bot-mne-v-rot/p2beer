package p2p

import org.junit.jupiter.api.*
import kotlin.test.*

import ru.emkn.p2beer.p2p.PeerId
import ru.emkn.p2beer.p2p.NodeId_sizeInBytes

class PeerIdTests {
    @Test
    fun `check for correct sizes`() {
        val random = PeerId.random()

        assertEquals(NodeId_sizeInBytes, PeerId.sizeInBytes)
        assertEquals(NodeId_sizeInBytes * UByte.SIZE_BITS, PeerId.sizeInBits)
        assertEquals(NodeId_sizeInBytes, random.data.size)
    }

    @Test
    fun `create NodeId with zeroes`() {
        val zeroes = PeerId.zeroes()

        zeroes.data.forEach { assertEquals(0u, it) }
    }

    @Test
    fun `create NodeId with max values`() {
        val max = PeerId.max()

        max.data.forEach { assertEquals(UByte.MAX_VALUE, it) }
    }

    @Test
    fun `correct toString`() {
        val id = PeerId.zeroes()
        id[0] = 5u
        id[1] = 16u

        val expected = """
            05.10.00.00.00.00.00.00.
            00.00.00.00.00.00.00.00.
            00.00.00.00.00.00.00.00.
            00.00.00.00.00.00.00.00"""
            .trimIndent()
            .split("\n", "\r\n")
            .joinToString("")

        assertEquals(expected, id.toString())
    }

    @Test
    fun `test xor`() {
        val a = PeerId.random()
        val b = PeerId.random()

        val xor = a xor b

        xor.data.forEachIndexed { index, res ->
            assertEquals(a[index] xor b[index], res)
        }
    }

    @Test
    fun `test bitwise and`() {
        val a = PeerId.random()
        val b = PeerId.random()

        val and = a and b

        and.data.forEachIndexed { index, res ->
            assertEquals(a[index] and b[index], res)
        }
    }

    @Test
    fun `test bitwise or`() {
        val a = PeerId.random()
        val b = PeerId.random()

        val or = a or b

        or.data.forEachIndexed { index, res ->
            assertEquals(a[index] or b[index], res)
        }
    }

    @Test
    fun `test bitwise inv`() {
        val orig = PeerId.random()
        val inv = orig.inv()

        (orig.data zip inv.data).forEach { (origByte, invByte) ->
            assertEquals(origByte.inv(), invByte)
        }
    }

    @Test
    fun `test bit at`() {
        val id = PeerId.zeroes()
        id[5] = 16u
        val pos = 5 * 8 + 3
        assertTrue(id.bitAt(pos))
        assertFalse(id.bitAt(pos + 1))
    }

    @Test
    fun `test LCP on different`() {
        val a = PeerId.zeroes()
        val b = PeerId.zeroes()

        a[5] = 80u
        b[5] = 72u

        assertEquals(5 * 8 + 3, a lcp b)
    }

    @Test
    fun `test LCP on equal`() {
        val a = PeerId.random()

        assertEquals(PeerId.sizeInBits, a lcp a)
    }

    @Test
    fun `test LCP within first byte`() {
        val a = PeerId.zeroes()
        val b = PeerId.zeroes()

        a[0] = (0b11111111).toUByte()
        b[0] = (0b10111111).toUByte()

        assertEquals(1, a lcp b)
    }
}