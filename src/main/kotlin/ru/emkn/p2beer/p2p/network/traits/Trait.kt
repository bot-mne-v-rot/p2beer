package ru.emkn.p2beer.p2p.network.traits

abstract class Trait {
    override fun equals(other: Any?): Boolean {
        return javaClass == other?.javaClass && other == this
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}