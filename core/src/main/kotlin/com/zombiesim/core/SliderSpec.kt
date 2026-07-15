package com.zombiesim.core

import kotlin.random.Random

/**
 * Metadata for a numeric setting: its valid range, default, and UI step size.
 * Lives as a companion-object constant next to the field it describes, so the
 * setup screen can build a slider from the same source of truth the data
 * class defaults come from.
 */
data class SliderSpec(
    val min: Float,
    val max: Float,
    val default: Float,
    val step: Float = 0.01f,
)

fun SliderSpec.randomValue(random: Random): Float = min + random.nextFloat() * (max - min)
