package ru.emkn.p2beer.p2p.network.traits

import ru.emkn.p2beer.p2p.network.Endpoint
import ru.emkn.p2beer.p2p.network.Transport

class Fast : Trait()
class Reliable : Trait()
class EnergyEfficient : Trait()

/**
 * Makes it possible to query for [Endpoint] support.
 */
class Supports(val transport: Transport) : Trait() {
    override fun equals(other: Any?): Boolean {
        return javaClass === other?.javaClass && transport == (other as Supports).transport
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + transport.hashCode()
        return result
    }
}