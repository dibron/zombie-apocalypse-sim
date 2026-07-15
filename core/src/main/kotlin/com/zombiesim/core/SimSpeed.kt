package com.zombiesim.core

import kotlinx.serialization.Serializable

@Serializable
enum class SimSpeed(val multiplier: Long, val label: String) {
    X1(1, "1x"),
    X10(10, "10x"),
    X100(100, "100x"),
    X1000(1000, "1000x"),
    X10000(10000, "10000x");

    companion object {
        fun fromMultiplier(multiplier: Long): SimSpeed =
            entries.firstOrNull { it.multiplier == multiplier }
                ?: throw IllegalArgumentException("Unsupported speed multiplier: $multiplier")
    }

    // scene2d's SelectBox renders items via toString().
    override fun toString(): String = label
}
