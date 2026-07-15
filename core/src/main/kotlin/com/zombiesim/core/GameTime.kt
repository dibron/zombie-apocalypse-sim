package com.zombiesim.core

/** A point in simulated time, derived from an absolute tick count (1 tick = 1 game hour). */
data class GameTime(val day: Long, val hour: Int)

fun Long.toGameTime(): GameTime = GameTime(day = this / 24, hour = (this % 24).toInt())
