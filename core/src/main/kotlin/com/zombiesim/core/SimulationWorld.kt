package com.zombiesim.core

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.configureWorld
import java.util.concurrent.atomic.AtomicLong

/** Proves the ECS is alive: every world step, this counter increments on a real Fleks entity. */
data class TickCounter(var ticks: Long = 0) : Component<TickCounter> {
    override fun type(): ComponentType<TickCounter> = TickCounter

    companion object : ComponentType<TickCounter>()
}

class TickCounterSystem : IteratingSystem(family = family { all(TickCounter) }) {
    override fun onTickEntity(entity: Entity) {
        entity[TickCounter].ticks++
    }
}

/**
 * Thin wrapper around a Fleks [World]. [step] is called once per [SimulationClock] tick,
 * on the clock's background thread - never on the render thread.
 */
class SimulationWorld {
    val world: World = configureWorld {
        systems {
            add(TickCounterSystem())
        }
    }

    private val clockEntity: Entity = world.entity { it += TickCounter() }

    // Mirrors the ECS-internal counter into a value the render thread can safely read
    // without touching Fleks state directly from a foreign thread.
    private val ecsTickMirror = AtomicLong(0)
    val ecsTickCount: Long get() = ecsTickMirror.get()

    fun step() {
        world.update(1f)
        ecsTickMirror.set(with(world) { clockEntity[TickCounter].ticks })
    }

    fun dispose() {
        world.dispose()
    }
}
