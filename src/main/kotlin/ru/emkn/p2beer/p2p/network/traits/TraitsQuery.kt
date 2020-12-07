package ru.emkn.p2beer.p2p.network.traits

import ru.emkn.p2beer.p2p.network.Endpoint
import ru.emkn.p2beer.p2p.network.StreamNode
import ru.emkn.p2beer.p2p.network.Transport

/**
 * Basic traits filtering making it easy to select
 * transport with desired properties.
 *
 * **Note:** Boolean operators seem to be pretty natural
 * but please put brackets when nesting them because
 * infix functions have no operator priority.
 */
abstract class TraitFilter {
    abstract fun test(traits: Collection<Trait>): Boolean

    infix fun and(other: TraitFilter): TraitFilter {
        val self = this

        return object : TraitFilter() {
            override fun test(traits: Collection<Trait>) =
                self.test(traits) && other.test(traits)
        }
    }

    infix fun or(other: TraitFilter): TraitFilter {
        val self = this

        return object : TraitFilter() {
            override fun test(traits: Collection<Trait>) =
                self.test(traits) || other.test(traits)
        }
    }

    fun not(): TraitFilter {
        val self = this

        return object : TraitFilter() {
            override fun test(traits: Collection<Trait>) =
                !self.test(traits)
        }
    }
}

val fast = object : TraitFilter() {
    override fun test(traits: Collection<Trait>): Boolean =
        Fast() in traits
}

val reliable = object : TraitFilter() {
    override fun test(traits: Collection<Trait>): Boolean =
        Reliable() in traits
}

val energyEfficient = object : TraitFilter() {
    override fun test(traits: Collection<Trait>): Boolean =
        EnergyEfficient() in traits
}

fun supports(endpoint: Endpoint) = object : TraitFilter() {
    override fun test(traits: Collection<Trait>): Boolean =
        traits.any { trait -> trait is Supports && trait.transport.supports(endpoint) }
}

@JvmName("filterStreams")
fun Collection<StreamNode>.filter(f: TraitFilter) =
    filter { node -> f.test(node.transport.traits) }

@JvmName("filterTransports")
fun Collection<Transport>.filter(f: TraitFilter) =
    filter { transport -> f.test(transport.descriptor.traits) }
