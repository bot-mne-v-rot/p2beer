package ru.emkn.p2beer.p2p.network.transports

import ru.emkn.p2beer.p2p.network.Endpoint
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress

class IPEndpoint {
    companion object {
        private val portRegexPattern = """
                (?<port>[0-9]|[1-9][0-9]|[0-9][0-9][0-9]|
                [0-9][0-9][0-9][0-9]|
                [0-5][0-9][0-9][0-9][0-9]|
                6[0-4][0-9][0-9][0-9]|
                65[0-4][0-9][0-9]|
                655[0-2][0-9]|
                6553[0-5])
                """
            .trimIndent().split('\n').joinToString("")

        val ipv4EndpointRegex: Regex by lazy {
            // https://mkyong.com/regular-expressions/how-to-validate-ip-address-with-regular-expression/
            val ipv4Pattern = "(?<ip>([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!:)|:)){4}"

            "/ipv4/$ipv4Pattern$portRegexPattern/".toRegex()
        }

        val ipv6EndpointRegex: Regex by lazy {
            // https://stackoverflow.com/questions/53497/regular-expression-that-matches-valid-ipv6-addresses
            val ipv6Pattern = """
                (?<ip>
                ([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|
                ([0-9a-fA-F]{1,4}:){1,7}:|
                ([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|
                ([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|
                ([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|
                ([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|
                ([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|
                [0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|
                :((:[0-9a-fA-F]{1,4}){1,7}|:)|
                fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|
                ::(ffff(:0{1,4}){0,1}:){0,1}
                ((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}
                (25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|
                ([0-9a-fA-F]{1,4}:){1,4}:
                ((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}
                (25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])
                )
            """.trimIndent().split('\n').joinToString("")

            "/ipv6/\\[$ipv6Pattern]:$portRegexPattern/".toRegex()
        }

        private fun isValidIPv4Endpoint(encoded: Endpoint): Boolean =
            ipv4EndpointRegex.matchEntire(encoded) != null

        private fun isValidIPv6Endpoint(encoded: Endpoint): Boolean =
            ipv6EndpointRegex.matchEntire(encoded) != null

        fun isValidEndpoint(encoded: Endpoint): Boolean =
            isValidIPv4Endpoint(encoded) || isValidIPv6Endpoint(encoded)

        private fun ipv4FromEndpoint(encoded: Endpoint): InetSocketAddress {
            val result = ipv4EndpointRegex.matchEntire(encoded)!!
            val addrStr = result.groups[1]!!.value.dropLast(1)
            val port = result.groups[4]!!.value.toInt()

            return InetSocketAddress(InetAddress.getByName(addrStr), port)
        }

        private fun ipv6FromEndpoint(encoded: Endpoint): InetSocketAddress {
            val result = ipv6EndpointRegex.matchEntire(encoded)!!
            val addrStr = result.groups[1]!!.value
            val port = result.groups[31]!!.value.toInt()

            return InetSocketAddress(InetAddress.getByName(addrStr), port)
        }

        fun fromEndpoint(encoded: Endpoint): InetSocketAddress =
            when {
                isValidIPv4Endpoint(encoded) -> ipv4FromEndpoint(encoded)
                isValidIPv6Endpoint(encoded) -> ipv6FromEndpoint(encoded)
                else -> throw IllegalArgumentException("Is not valid endpoint.")
            }

        private fun ipv4ToEndpoint(address: InetSocketAddress): Endpoint =
            "/ipv4/${address.address.address.joinToString(".") { it.toString() }}:${address.port}/"

        private fun ipv6ToEndpoint(address: InetSocketAddress): Endpoint =
            "/ipv6/[${address.address.toString().split('/').last()}]:${address.port}/"

        fun toEndpoint(address: InetSocketAddress): Endpoint =
            when (address.address) {
                is Inet4Address -> ipv4ToEndpoint(address)
                else -> ipv6ToEndpoint(address)
            }

    }
}