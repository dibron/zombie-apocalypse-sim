package com.zombiesim.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable

/**
 * Hand-rolled scene2d.ui styles built from solid-color drawables, so Phase 1/2 need no
 * external skin/atlas asset files. Swap for a real skin once art direction exists.
 */
class BasicUi : Disposable {
    private val pixelTexture: Texture = run {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        texture
    }

    val font = BitmapFont()

    private fun solid(color: Color): Drawable = TextureRegionDrawable(TextureRegion(pixelTexture)).tint(color)

    private fun solid(color: Color, minWidth: Float, minHeight: Float): Drawable =
        solid(color).apply {
            this.minWidth = minWidth
            this.minHeight = minHeight
        }

    val labelStyle = Label.LabelStyle(font, Color.WHITE)
    val errorLabelStyle = Label.LabelStyle(font, Color(0.9f, 0.35f, 0.35f, 1f))
    val okLabelStyle = Label.LabelStyle(font, Color(0.45f, 0.85f, 0.45f, 1f))

    val buttonStyle = TextButton.TextButtonStyle().apply {
        up = solid(Color.DARK_GRAY)
        down = solid(Color.GRAY)
        checked = solid(Color(0.3f, 0.45f, 0.3f, 1f))
        font = this@BasicUi.font
        fontColor = Color.WHITE
    }

    val selectBoxStyle = SelectBox.SelectBoxStyle().apply {
        font = this@BasicUi.font
        fontColor = Color.WHITE
        background = solid(Color.DARK_GRAY)
        scrollStyle = ScrollPane.ScrollPaneStyle()
        listStyle = List.ListStyle().apply {
            font = this@BasicUi.font
            fontColorSelected = Color.WHITE
            fontColorUnselected = Color.LIGHT_GRAY
            selection = solid(Color.GRAY)
        }
    }

    val sliderStyle = Slider.SliderStyle().apply {
        background = solid(Color.DARK_GRAY, minWidth = 0f, minHeight = 6f)
        knob = solid(Color.LIGHT_GRAY, minWidth = 14f, minHeight = 14f)
    }

    val checkBoxStyle = CheckBox.CheckBoxStyle().apply {
        checkboxOff = solid(Color.DARK_GRAY, minWidth = 18f, minHeight = 18f)
        checkboxOn = solid(Color(0.35f, 0.75f, 0.35f, 1f), minWidth = 18f, minHeight = 18f)
        font = this@BasicUi.font
        fontColor = Color.WHITE
    }

    val textFieldStyle = TextField.TextFieldStyle().apply {
        font = this@BasicUi.font
        fontColor = Color.WHITE
        cursor = solid(Color.WHITE, minWidth = 2f, minHeight = 20f)
        selection = solid(Color(0.3f, 0.3f, 0.6f, 1f))
        background = solid(Color(0.15f, 0.15f, 0.18f, 1f), minWidth = 10f, minHeight = 24f)
    }

    val windowStyle = Window.WindowStyle(font, Color.WHITE, solid(Color(0.12f, 0.12f, 0.15f, 0.98f)))

    override fun dispose() {
        pixelTexture.dispose()
        font.dispose()
    }
}
