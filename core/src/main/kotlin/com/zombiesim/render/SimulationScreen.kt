package com.zombiesim.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.zombiesim.core.SimulationClock
import com.zombiesim.core.SimulationWorld
import com.zombiesim.core.WorldConfig
import com.zombiesim.ui.BasicUi
import com.zombiesim.ui.SimulationHud

private const val GRID_SPACING_PX = 32f
private val GRID_COLOR = Color(0.25f, 0.25f, 0.3f, 1f)
private val BACKGROUND_COLOR = Color(0.08f, 0.08f, 0.1f, 1f)

/**
 * Render-thread owner. Its render loop runs at whatever the display/vsync gives it and
 * never waits on [SimulationClock] - it just reads the latest tick/ECS counters each frame,
 * however far ahead (or behind, while paused) the sim happens to be.
 *
 * [worldConfig] is the finalized configuration handed off from [SetupScreen]. Nothing
 * consumes it yet - Phase 3 (world generation) is what will read it.
 */
class SimulationScreen(
    @Suppress("unused") private val worldConfig: WorldConfig,
) : ScreenAdapter() {
    private val basicUi = BasicUi()
    private val simulationWorld = SimulationWorld()
    private val clock = SimulationClock(onTick = { simulationWorld.step() })
    private val hud = SimulationHud(clock, basicUi)
    private val shapeRenderer = ShapeRenderer()
    private val camera = OrthographicCamera()

    override fun show() {
        Gdx.input.inputProcessor = hud.stage
        clock.start()
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        hud.resize(width, height)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, BACKGROUND_COLOR.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        drawGrid()

        hud.update(ecsTickCount = simulationWorld.ecsTickCount, fps = Gdx.graphics.framesPerSecond)
        hud.stage.draw()
    }

    private fun drawGrid() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = GRID_COLOR
        var x = 0f
        while (x <= camera.viewportWidth) {
            shapeRenderer.line(x, 0f, x, camera.viewportHeight)
            x += GRID_SPACING_PX
        }
        var y = 0f
        while (y <= camera.viewportHeight) {
            shapeRenderer.line(0f, y, camera.viewportWidth, y)
            y += GRID_SPACING_PX
        }
        shapeRenderer.end()
    }

    override fun dispose() {
        clock.shutdown()
        simulationWorld.dispose()
        shapeRenderer.dispose()
        hud.dispose()
        basicUi.dispose()
    }
}
