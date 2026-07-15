package com.zombiesim.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.zombiesim.render.ZombieSimGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("zombie-sim")
        setWindowedMode(1024, 768)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(ZombieSimGame(), config)
}
