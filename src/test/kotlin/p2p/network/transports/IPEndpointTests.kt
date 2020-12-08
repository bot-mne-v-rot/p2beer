package p2p.network.transports

import org.junit.jupiter.api.*
import ru.emkn.p2beer.p2p.network.transports.IPEndpoint
import kotlin.test.*

class IPEndpointTests {
    @Test
    fun `test ipv6 parsing`() {
        IPEndpoint.fromEndpoint("/ipv6/[0::0]:4000/")
    }

    @Test
    fun `test ipv4 parsing`() {
        IPEndpoint.fromEndpoint("/ipv4/0.0.0.0:4000/")
    }

    @Test
    fun `test ipv4 encoding`() {
        val addr1 = IPEndpoint.fromEndpoint("/ipv4/0.0.0.0:4000/")
        val addr2 = IPEndpoint.fromEndpoint(IPEndpoint.toEndpoint(addr1))
        assertEquals(addr2, addr1)
    }

    @Test
    fun `test ipv6 encoding`() {
        val addr1 = IPEndpoint.fromEndpoint("/ipv6/[0::0]:4000/")
        val addr2 = IPEndpoint.fromEndpoint(IPEndpoint.toEndpoint(addr1))
        assertEquals(addr2, addr1)
    }
}