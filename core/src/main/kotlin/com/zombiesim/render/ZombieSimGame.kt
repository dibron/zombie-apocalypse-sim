package com.zombiesim.render

import com.badlogic.gdx.Game

class ZombieSimGame : Game() {
    override fun create() {
        // Game.setScreen() doesn't dispose the outgoing screen, so dispose SetupScreen
        // (and its Stage/BasicUi textures) ourselves once we've handed off to the sim.
        lateinit var setupScreen: SetupScreen
        setupScreen = SetupScreen(onStart = { config ->
            setScreen(SimulationScreen(config))
            setupScreen.dispose()
        })
        setScreen(setupScreen)
    }
}
