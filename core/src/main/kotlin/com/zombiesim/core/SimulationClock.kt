package com.zombiesim.core

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Drives the simulation forward in discrete ticks (1 tick = 1 game hour) on its own
 * background thread, completely decoupled from the render thread. The render loop
 * only ever reads [currentTick] / [isRunning] / [speed] - it never blocks on this clock,
 * and this clock never waits on rendering.
 *
 * Ticks are produced by accumulating real elapsed time scaled by the speed multiplier and
 * draining whole ticks from that accumulator each loop iteration, rather than sleeping per
 * tick. That lets speeds like 10000x emit many ticks per iteration instead of requiring
 * sub-millisecond sleeps the OS scheduler can't reliably deliver.
 */
class SimulationClock(
    initialSpeed: SimSpeed = SimSpeed.X1,
    private val onTick: (tick: Long) -> Unit,
) {
    private companion object {
        const val TICK_NANOS = 1_000_000_000L // 1 game hour of sim-time per second of real-time at 1x
        const val LOOP_SLEEP_MILLIS = 1L
    }

    private val running = AtomicBoolean(false)
    private val speedMultiplier = AtomicLong(initialSpeed.multiplier)
    private val tickCount = AtomicLong(0)

    @Volatile private var worker: Thread? = null
    @Volatile private var shutdownRequested = false

    val currentTick: Long get() = tickCount.get()
    val isRunning: Boolean get() = running.get()
    val speed: SimSpeed get() = SimSpeed.fromMultiplier(speedMultiplier.get())

    fun start() {
        if (worker != null) return
        shutdownRequested = false
        worker = Thread(::runLoop, "simulation-clock").apply {
            isDaemon = true
            start()
        }
    }

    fun shutdown() {
        shutdownRequested = true
        worker?.join(1000)
        worker = null
    }

    fun play() {
        running.set(true)
    }

    fun pause() {
        running.set(false)
    }

    fun togglePlayPause() {
        running.set(!running.get())
    }

    fun setSpeed(newSpeed: SimSpeed) {
        speedMultiplier.set(newSpeed.multiplier)
    }

    private fun runLoop() {
        var lastNanos = System.nanoTime()
        var accumulatedNanos = 0L

        while (!shutdownRequested) {
            val now = System.nanoTime()
            val deltaNanos = now - lastNanos
            lastNanos = now

            if (running.get()) {
                accumulatedNanos += deltaNanos * speedMultiplier.get()
                while (accumulatedNanos >= TICK_NANOS) {
                    accumulatedNanos -= TICK_NANOS
                    onTick(tickCount.incrementAndGet())
                }
            } else {
                accumulatedNanos = 0L
            }

            try {
                Thread.sleep(LOOP_SLEEP_MILLIS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
    }
}
