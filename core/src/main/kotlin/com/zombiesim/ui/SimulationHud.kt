package com.zombiesim.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.zombiesim.core.SimSpeed
import com.zombiesim.core.SimulationClock
import com.zombiesim.core.toGameTime

/** scene2d.ui controls (play/pause, speed selector) plus a debug readout, wired to a [SimulationClock]. */
class SimulationHud(private val clock: SimulationClock, ui: BasicUi) {

    val stage = Stage(ScreenViewport())

    private val playPauseButton = TextButton("Play", ui.buttonStyle)
    private val speedSelect = SelectBox<SimSpeed>(ui.selectBoxStyle).apply {
        setItems(*SimSpeed.entries.toTypedArray())
        selected = SimSpeed.X1
    }
    private val debugLabel = Label("", ui.labelStyle).apply { setAlignment(Align.left) }

    init {
        playPauseButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                clock.togglePlayPause()
                refreshPlayPauseLabel()
            }
        })
        speedSelect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                clock.setSpeed(speedSelect.selected)
            }
        })

        val root = Table()
        root.setFillParent(true)
        root.top().left().pad(10f)

        val controls = Table()
        controls.add(playPauseButton).padRight(10f)
        controls.add(Label("Speed:", ui.labelStyle)).padRight(4f)
        controls.add(speedSelect)

        root.add(controls).left().row()
        root.add(debugLabel).left().padTop(8f)

        stage.addActor(root)
        refreshPlayPauseLabel()
    }

    private fun refreshPlayPauseLabel() {
        playPauseButton.setText(if (clock.isRunning) "Pause" else "Play")
    }

    /** Called every render frame from the render thread only. */
    fun update(ecsTickCount: Long, fps: Int) {
        refreshPlayPauseLabel()
        val time = clock.currentTick.toGameTime()
        debugLabel.setText(
            "tick=${clock.currentTick}  day=${time.day} hour=${time.hour}  " +
                "ecsTicks=$ecsTickCount  speed=${clock.speed.label}  fps=$fps"
        )
        stage.act()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    fun dispose() {
        stage.dispose()
    }
}
