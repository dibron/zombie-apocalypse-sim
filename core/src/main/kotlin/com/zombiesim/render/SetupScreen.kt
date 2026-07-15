package com.zombiesim.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.zombiesim.core.CitySize
import com.zombiesim.core.DifficultyPreset
import com.zombiesim.core.MilitaryPresence
import com.zombiesim.core.OutbreakLocation
import com.zombiesim.core.Season
import com.zombiesim.core.SimSpeed
import com.zombiesim.core.SimulationLengthCap
import com.zombiesim.core.SliderSpec
import com.zombiesim.core.WorldConfig
import com.zombiesim.core.WorldConfigPresets
import com.zombiesim.core.isBalancedPercentage
import com.zombiesim.core.percentageSum
import com.zombiesim.ui.BasicUi
import kotlin.math.abs

// See PLAN.md "Scale & Performance Targets": no aggregation systems exist until Phase 9/18,
// so 1000x/10000x speeds above this population are flagged as likely to slow down.
private const val PERFORMANCE_WARNING_POPULATION_THRESHOLD = 30_000

/**
 * Pre-simulation configuration screen. Builds [WorldConfig] via [config], a mutable snapshot
 * updated as the user edits controls, and hands the final value to [onStart] when they click
 * "Generate & Start". Nothing here talks to [SimulationClock] or Fleks - this is pure UI +
 * data, matching Phase 2 scope.
 */
class SetupScreen(
    private val onStart: (WorldConfig) -> Unit,
) : ScreenAdapter() {

    private val basicUi = BasicUi()
    private val presets = WorldConfigPresets()
    private val stage = Stage(ScreenViewport())

    private var config: WorldConfig = WorldConfig.default()
    private val statusLabel = Label("", basicUi.labelStyle)

    private val tabs: List<Pair<String, () -> Table>> = listOf(
        "City & Map" to ::buildCityMapTab,
        "Infection" to ::buildInfectionTab,
        "Population" to ::buildPopulationTab,
        "Economy" to ::buildEconomyTab,
        "Military" to ::buildMilitaryTab,
        "Weather" to ::buildWeatherTab,
        "Noise & Combat" to ::buildNoiseAndCombatTab,
        "Meta" to ::buildMetaTab,
    )
    private var activeTabIndex = 0

    override fun show() {
        Gdx.input.inputProcessor = stage
        rebuildUi()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        basicUi.dispose()
    }

    // ---------------------------------------------------------------- layout

    private fun rebuildUi() {
        stage.clear()

        val root = Table()
        root.setFillParent(true)
        root.top()
        stage.addActor(root)

        val tabBar = Table()
        val tabGroup = ButtonGroup<TextButton>().apply {
            setMinCheckCount(1)
            setMaxCheckCount(1)
        }
        val contentHolder = Table()

        fun showTab(index: Int) {
            activeTabIndex = index
            contentHolder.clear()
            val tabTable = tabs[index].second()
            val scrollPane = ScrollPane(tabTable, ScrollPane.ScrollPaneStyle())
            scrollPane.setFadeScrollBars(false)
            contentHolder.add(scrollPane).grow()
        }

        tabs.forEachIndexed { index, (name, _) ->
            val button = TextButton(name, basicUi.buttonStyle)
            button.isChecked = index == activeTabIndex
            tabGroup.add(button)
            button.onChange { if (button.isChecked) showTab(index) }
            tabBar.add(button).pad(2f)
        }

        root.add(tabBar).top().padTop(8f).row()
        root.add(contentHolder).grow().padTop(6f).row()
        root.add(buildBottomBar()).padTop(8f).row()
        root.add(statusLabel).padTop(4f).padBottom(8f)

        showTab(activeTabIndex)
    }

    private fun buildBottomBar(): Table {
        val bar = Table()

        val randomizeButton = TextButton("Randomize All", basicUi.buttonStyle)
        randomizeButton.onChange {
            config = WorldConfig.randomized()
            statusLabel.style = basicUi.labelStyle
            statusLabel.setText("Randomized.")
            rebuildUi()
        }

        val resetButton = TextButton("Reset to Default", basicUi.buttonStyle)
        resetButton.onChange {
            config = WorldConfig.default()
            statusLabel.style = basicUi.labelStyle
            statusLabel.setText("Reset to defaults.")
            rebuildUi()
        }

        val saveButton = TextButton("Save Preset", basicUi.buttonStyle)
        saveButton.onChange { showSavePresetDialog() }

        val loadButton = TextButton("Load Preset", basicUi.buttonStyle)
        loadButton.onChange { showLoadPresetDialog() }

        val startButton = TextButton("Generate & Start", basicUi.buttonStyle)
        startButton.onChange { attemptStart() }

        bar.add(randomizeButton).pad(4f)
        bar.add(resetButton).pad(4f)
        bar.add(saveButton).pad(4f)
        bar.add(loadButton).pad(4f)
        bar.add(startButton).pad(4f)
        return bar
    }

    private fun attemptStart() {
        if (!isConfigValid()) {
            statusLabel.style = basicUi.errorLabelStyle
            statusLabel.setText("Fix unbalanced percentages (district mix / professions / personalities) before starting.")
            return
        }
        onStart(config)
    }

    private fun isConfigValid(): Boolean =
        config.cityMap.districtMix.isBalancedPercentage() &&
            config.population.professionDistribution.isBalancedPercentage() &&
            config.population.personalityDistribution.isBalancedPercentage()

    private fun showSavePresetDialog() {
        val nameField = TextField("", basicUi.textFieldStyle)
        val dialog = object : Dialog("Save Preset", basicUi.windowStyle) {
            override fun result(obj: Any?) {
                if (obj == true) {
                    val name = nameField.text.trim()
                    if (name.isNotEmpty()) {
                        presets.save(name, config)
                        statusLabel.style = basicUi.okLabelStyle
                        statusLabel.setText("Saved preset '$name'.")
                    }
                }
            }
        }
        dialog.contentTable.add(Label("Preset name:", basicUi.labelStyle)).left().padRight(8f)
        dialog.contentTable.add(nameField).width(220f)
        dialog.button("Save", true)
        dialog.button("Cancel", false)
        dialog.show(stage)
        stage.keyboardFocus = nameField
    }

    private fun showLoadPresetDialog() {
        val names = presets.listPresetNames()
        val dialog = Dialog("Load Preset", basicUi.windowStyle)
        if (names.isEmpty()) {
            dialog.contentTable.add(Label("No saved presets.", basicUi.labelStyle))
        } else {
            names.forEach { name ->
                val presetButton = TextButton(name, basicUi.buttonStyle)
                presetButton.onChange {
                    presets.load(name)?.let { loaded ->
                        config = loaded
                        statusLabel.style = basicUi.okLabelStyle
                        statusLabel.setText("Loaded preset '$name'.")
                        rebuildUi()
                    }
                    dialog.hide()
                }
                dialog.contentTable.add(presetButton).width(220f).padBottom(4f).row()
            }
        }
        dialog.button("Close")
        dialog.show(stage)
    }

    // ---------------------------------------------------------------- tabs

    private fun buildCityMapTab(): Table {
        val t = newTabTable()
        val cm = config.cityMap

        t.addEnumRow("City size", CitySize.entries, cm.citySize) {
            config = config.copy(cityMap = config.cityMap.copy(citySize = it))
        }
        t.addSliderRow("District density", WorldConfig.CityMap.DISTRICT_DENSITY, cm.districtDensity) {
            config = config.copy(cityMap = config.cityMap.copy(districtDensity = it))
        }
        t.addSliderRow("Number of exits", WorldConfig.CityMap.NUM_EXITS, cm.numExits.toFloat(), isInt = true) {
            config = config.copy(cityMap = config.cityMap.copy(numExits = it.toInt()))
        }
        t.addToggleRow("Metro enabled", cm.metroEnabled) {
            config = config.copy(cityMap = config.cityMap.copy(metroEnabled = it))
        }
        t.addSliderRow("Starting population", WorldConfig.CityMap.STARTING_POPULATION, cm.startingPopulation.toFloat(), isInt = true) {
            config = config.copy(cityMap = config.cityMap.copy(startingPopulation = it.toInt()))
        }
        t.addTextFieldRow("Map seed", cm.mapSeed) {
            config = config.copy(cityMap = config.cityMap.copy(mapSeed = it))
        }
        t.addPercentageMapSection(
            "District mix (%)",
            cm.districtMix,
            WorldConfig.CityMap.DISTRICT_PERCENTAGE,
            { it.name },
        ) { key, value ->
            config = config.copy(
                cityMap = config.cityMap.copy(
                    districtMix = config.cityMap.districtMix.toMutableMap().apply { this[key] = value },
                ),
            )
        }
        return t
    }

    private fun buildInfectionTab(): Table {
        val t = newTabTable()
        val inf = config.infection

        t.addToggleRow("Reanimate on any death", inf.reanimateOnAnyDeath) {
            config = config.copy(infection = config.infection.copy(reanimateOnAnyDeath = it))
        }
        t.addSliderRow("Incubation min (hrs)", WorldConfig.Infection.INCUBATION_MIN_HOURS, inf.incubationMinHours.toFloat(), isInt = true) {
            config = config.copy(infection = config.infection.copy(incubationMinHours = it.toInt()))
        }
        t.addSliderRow("Incubation max (hrs)", WorldConfig.Infection.INCUBATION_MAX_HOURS, inf.incubationMaxHours.toFloat(), isInt = true) {
            config = config.copy(infection = config.infection.copy(incubationMaxHours = it.toInt()))
        }
        t.addSliderRow("Bite lethality", WorldConfig.Infection.BITE_LETHALITY, inf.biteLethality) {
            config = config.copy(infection = config.infection.copy(biteLethality = it))
        }
        t.addSliderRow("Zombie base speed", WorldConfig.Infection.ZOMBIE_BASE_SPEED, inf.zombieBaseSpeed) {
            config = config.copy(infection = config.infection.copy(zombieBaseSpeed = it))
        }
        t.addSliderRow("Zombie decay rate", WorldConfig.Infection.ZOMBIE_DECAY_RATE, inf.zombieDecayRate) {
            config = config.copy(infection = config.infection.copy(zombieDecayRate = it))
        }
        t.addToggleRow("Mutation enabled", inf.mutationEnabled) {
            config = config.copy(infection = config.infection.copy(mutationEnabled = it))
        }
        t.addSliderRow("Mutation speed", WorldConfig.Infection.MUTATION_SPEED, inf.mutationSpeed) {
            config = config.copy(infection = config.infection.copy(mutationSpeed = it))
        }
        t.addToggleRow("Airborne mutation possible", inf.airborneMutationPossible) {
            config = config.copy(infection = config.infection.copy(airborneMutationPossible = it))
        }
        t.addSliderRow("Starting outbreak size", WorldConfig.Infection.STARTING_OUTBREAK_SIZE, inf.startingOutbreakSize) {
            config = config.copy(infection = config.infection.copy(startingOutbreakSize = it))
        }
        t.addEnumRow("Starting outbreak location", OutbreakLocation.entries, inf.startingOutbreakLocation) {
            config = config.copy(infection = config.infection.copy(startingOutbreakLocation = it))
        }
        t.addWeightMapSection(
            "Zombie type spawn weights (relative, need not total 100)",
            inf.zombieTypeSpawnWeights,
            WorldConfig.Infection.ZOMBIE_WEIGHT,
            { it.name },
        ) { key, value ->
            config = config.copy(
                infection = config.infection.copy(
                    zombieTypeSpawnWeights = config.infection.zombieTypeSpawnWeights.toMutableMap().apply { this[key] = value },
                ),
            )
        }
        return t
    }

    private fun buildPopulationTab(): Table {
        val t = newTabTable()
        val pop = config.population

        t.addSliderRow("Average age", WorldConfig.Population.AVERAGE_AGE, pop.averageAge.toFloat(), isInt = true) {
            config = config.copy(population = config.population.copy(averageAge = it.toInt()))
        }
        t.addSliderRow("Age spread", WorldConfig.Population.AGE_SPREAD, pop.ageSpread.toFloat(), isInt = true) {
            config = config.copy(population = config.population.copy(ageSpread = it.toInt()))
        }
        t.addSliderRow("Starting morale", WorldConfig.Population.STARTING_MORALE, pop.startingMorale) {
            config = config.copy(population = config.population.copy(startingMorale = it))
        }
        t.addSliderRow("Starting fear sensitivity", WorldConfig.Population.STARTING_FEAR_SENSITIVITY, pop.startingFearSensitivity) {
            config = config.copy(population = config.population.copy(startingFearSensitivity = it))
        }
        t.addSliderRow("Loyalty baseline", WorldConfig.Population.LOYALTY_BASELINE, pop.loyaltyBaseline) {
            config = config.copy(population = config.population.copy(loyaltyBaseline = it))
        }
        t.addSliderRow("Social network density", WorldConfig.Population.SOCIAL_NETWORK_DENSITY, pop.socialNetworkDensity) {
            config = config.copy(population = config.population.copy(socialNetworkDensity = it))
        }
        t.addSliderRow("Weapon ownership rate", WorldConfig.Population.WEAPON_OWNERSHIP_RATE, pop.weaponOwnershipRate) {
            config = config.copy(population = config.population.copy(weaponOwnershipRate = it))
        }
        t.addPercentageMapSection(
            "Profession distribution (%)",
            pop.professionDistribution,
            WorldConfig.Population.DISTRIBUTION_PERCENTAGE,
            { it.name },
        ) { key, value ->
            config = config.copy(
                population = config.population.copy(
                    professionDistribution = config.population.professionDistribution.toMutableMap().apply { this[key] = value },
                ),
            )
        }
        t.addPercentageMapSection(
            "Personality distribution (%)",
            pop.personalityDistribution,
            WorldConfig.Population.DISTRIBUTION_PERCENTAGE,
            { it.name },
        ) { key, value ->
            config = config.copy(
                population = config.population.copy(
                    personalityDistribution = config.population.personalityDistribution.toMutableMap().apply { this[key] = value },
                ),
            )
        }
        return t
    }

    private fun buildEconomyTab(): Table {
        val t = newTabTable()
        val econ = config.economy
        val abundance = econ.startingAbundance

        t.row()
        t.add(Label("Starting resource abundance", basicUi.labelStyle)).left().colspan(3).padTop(4f)
        t.addSliderRow("  Food", WorldConfig.ResourceAbundance.SPEC, abundance.food) {
            config = config.copy(economy = config.economy.copy(startingAbundance = config.economy.startingAbundance.copy(food = it)))
        }
        t.addSliderRow("  Water", WorldConfig.ResourceAbundance.SPEC, abundance.water) {
            config = config.copy(economy = config.economy.copy(startingAbundance = config.economy.startingAbundance.copy(water = it)))
        }
        t.addSliderRow("  Medicine", WorldConfig.ResourceAbundance.SPEC, abundance.medicine) {
            config = config.copy(economy = config.economy.copy(startingAbundance = config.economy.startingAbundance.copy(medicine = it)))
        }
        t.addSliderRow("  Fuel", WorldConfig.ResourceAbundance.SPEC, abundance.fuel) {
            config = config.copy(economy = config.economy.copy(startingAbundance = config.economy.startingAbundance.copy(fuel = it)))
        }
        t.addSliderRow("  Ammo", WorldConfig.ResourceAbundance.SPEC, abundance.ammo) {
            config = config.copy(economy = config.economy.copy(startingAbundance = config.economy.startingAbundance.copy(ammo = it)))
        }
        t.addSliderRow("Consumption rate", WorldConfig.Economy.CONSUMPTION_RATE, econ.consumptionRate) {
            config = config.copy(economy = config.economy.copy(consumptionRate = it))
        }
        t.addToggleRow("Trading enabled", econ.tradingEnabled) {
            config = config.copy(economy = config.economy.copy(tradingEnabled = it))
        }
        t.addSliderRow("Scarcity violence threshold", WorldConfig.Economy.SCARCITY_VIOLENCE_THRESHOLD, econ.scarcityViolenceThreshold) {
            config = config.copy(economy = config.economy.copy(scarcityViolenceThreshold = it))
        }
        t.addSliderRow("Production efficiency", WorldConfig.Economy.PRODUCTION_EFFICIENCY, econ.productionEfficiency) {
            config = config.copy(economy = config.economy.copy(productionEfficiency = it))
        }
        return t
    }

    private fun buildMilitaryTab(): Table {
        val t = newTabTable()
        val mil = config.military

        t.addEnumRow("Presence level", MilitaryPresence.entries, mil.presenceLevel) {
            config = config.copy(military = config.military.copy(presenceLevel = it))
        }
        t.addSliderRow("Response delay (hrs)", WorldConfig.Military.RESPONSE_DELAY_HOURS, mil.responseDelayHours.toFloat(), isInt = true) {
            config = config.copy(military = config.military.copy(responseDelayHours = it.toInt()))
        }
        t.addSliderRow("Stage aggression", WorldConfig.Military.STAGE_AGGRESSION, mil.stageAggression) {
            config = config.copy(military = config.military.copy(stageAggression = it))
        }
        t.addSliderRow("Police competence", WorldConfig.Military.POLICE_COMPETENCE, mil.policeCompetence) {
            config = config.copy(military = config.military.copy(policeCompetence = it))
        }
        t.addToggleRow("Evacuation protocol enabled", mil.evacuationProtocolEnabled) {
            config = config.copy(military = config.military.copy(evacuationProtocolEnabled = it))
        }
        return t
    }

    private fun buildWeatherTab(): Table {
        val t = newTabTable()
        val weather = config.weather

        t.addEnumRow("Starting season", Season.entries, weather.startingSeason) {
            config = config.copy(weather = config.weather.copy(startingSeason = it))
        }
        t.addSliderRow("Weather volatility", WorldConfig.Weather.WEATHER_VOLATILITY, weather.weatherVolatility) {
            config = config.copy(weather = config.weather.copy(weatherVolatility = it))
        }
        t.addSliderRow("Winter severity", WorldConfig.Weather.WINTER_SEVERITY, weather.winterSeverity) {
            config = config.copy(weather = config.weather.copy(winterSeverity = it))
        }
        t.addToggleRow("Disaster events enabled", weather.disasterEventsEnabled) {
            config = config.copy(weather = config.weather.copy(disasterEventsEnabled = it))
        }
        return t
    }

    private fun buildNoiseAndCombatTab(): Table {
        val t = newTabTable()
        val nc = config.noiseAndCombat

        t.addSliderRow("Noise propagation range", WorldConfig.NoiseAndCombat.NOISE_PROPAGATION_RANGE, nc.noisePropagationRange) {
            config = config.copy(noiseAndCombat = config.noiseAndCombat.copy(noisePropagationRange = it))
        }
        t.addSliderRow("Gunfire attraction multiplier", WorldConfig.NoiseAndCombat.GUNFIRE_ATTRACTION_MULTIPLIER, nc.gunfireAttractionMultiplier) {
            config = config.copy(noiseAndCombat = config.noiseAndCombat.copy(gunfireAttractionMultiplier = it))
        }
        t.addSliderRow("Barricade effectiveness", WorldConfig.NoiseAndCombat.BARRICADE_EFFECTIVENESS, nc.barricadeEffectiveness) {
            config = config.copy(noiseAndCombat = config.noiseAndCombat.copy(barricadeEffectiveness = it))
        }
        t.addSliderRow("Combat lethality", WorldConfig.NoiseAndCombat.COMBAT_LETHALITY, nc.combatLethality) {
            config = config.copy(noiseAndCombat = config.noiseAndCombat.copy(combatLethality = it))
        }
        t.addToggleRow("Friendly fire enabled", nc.friendlyFireEnabled) {
            config = config.copy(noiseAndCombat = config.noiseAndCombat.copy(friendlyFireEnabled = it))
        }
        return t
    }

    private fun buildMetaTab(): Table {
        val t = newTabTable()
        val meta = config.meta

        t.addEnumRow("Simulation length cap", SimulationLengthCap.entries, meta.simulationLengthCap) {
            config = config.copy(meta = config.meta.copy(simulationLengthCap = it))
        }
        t.addSliderRow("Random event frequency", WorldConfig.Meta.RANDOM_EVENT_FREQUENCY, meta.randomEventFrequency) {
            config = config.copy(meta = config.meta.copy(randomEventFrequency = it))
        }
        t.addEnumRow("Difficulty preset", DifficultyPreset.entries, meta.difficultyPreset) {
            config = config.copy(meta = config.meta.copy(difficultyPreset = it))
        }
        val warningLabel = Label("", basicUi.labelStyle).apply { wrap = true }

        fun refreshPerformanceWarning() {
            val population = config.cityMap.startingPopulation
            val highSpeedEnabled = SimSpeed.X1000 in config.meta.enabledSpeedPresets ||
                SimSpeed.X10000 in config.meta.enabledSpeedPresets
            if (population > PERFORMANCE_WARNING_POPULATION_THRESHOLD && highSpeedEnabled) {
                warningLabel.style = basicUi.errorLabelStyle
                warningLabel.setText(
                    "Warning: starting population ($population) exceeds the recommended " +
                        "~30,000 cap for Phases 1-8 with 1000x/10000x speeds enabled - no horde " +
                        "aggregation exists yet (see PLAN.md \"Scale & Performance Targets\"). " +
                        "Expect slowdowns at high speed.",
                )
            } else {
                warningLabel.style = basicUi.labelStyle
                warningLabel.setText("")
            }
        }

        t.addSpeedSetRow(meta.enabledSpeedPresets) {
            config = config.copy(meta = config.meta.copy(enabledSpeedPresets = it))
            refreshPerformanceWarning()
        }

        t.row()
        t.add(warningLabel).left().colspan(3).width(520f).padTop(10f)
        refreshPerformanceWarning()

        return t
    }

    // ---------------------------------------------------------------- row builders

    private fun newTabTable(): Table = Table().apply { top(); left(); pad(12f); defaults().padBottom(6f) }

    private fun Actor.onChange(action: () -> Unit) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) = action()
        })
    }

    private fun formatValue(value: Float, isInt: Boolean): String =
        if (isInt) value.toInt().toString() else "%.2f".format(value)

    private fun Table.addSliderRow(
        label: String,
        spec: SliderSpec,
        current: Float,
        isInt: Boolean = false,
        onChangeValue: (Float) -> Unit,
    ) {
        row()
        val slider = Slider(spec.min, spec.max, spec.step, false, basicUi.sliderStyle)
        slider.value = current
        val valueLabel = Label(formatValue(current, isInt), basicUi.labelStyle)
        slider.onChange {
            valueLabel.setText(formatValue(slider.value, isInt))
            onChangeValue(slider.value)
        }
        add(Label(label, basicUi.labelStyle)).left().width(240f).padRight(8f)
        add(slider).width(260f).padRight(8f)
        add(valueLabel).left().width(60f)
    }

    private fun Table.addToggleRow(label: String, current: Boolean, onChangeValue: (Boolean) -> Unit) {
        row()
        val checkBox = CheckBox(" $label", basicUi.checkBoxStyle)
        checkBox.isChecked = current
        checkBox.onChange { onChangeValue(checkBox.isChecked) }
        add(checkBox).left().colspan(3)
    }

    private inline fun <reified T> Table.addEnumRow(label: String, options: List<T>, current: T, crossinline onChangeValue: (T) -> Unit) {
        row()
        val select = SelectBox<T>(basicUi.selectBoxStyle)
        select.setItems(*options.toTypedArray())
        select.selected = current
        select.onChange { onChangeValue(select.selected) }
        add(Label(label, basicUi.labelStyle)).left().width(240f).padRight(8f)
        add(select).left().colspan(2).width(200f)
    }

    private fun Table.addTextFieldRow(label: String, current: String, onChangeValue: (String) -> Unit) {
        row()
        val field = TextField(current, basicUi.textFieldStyle)
        field.onChange { onChangeValue(field.text) }
        add(Label(label, basicUi.labelStyle)).left().width(240f).padRight(8f)
        add(field).left().colspan(2).width(200f)
    }

    private fun <K> Table.addPercentageMapSection(
        title: String,
        map: Map<K, Float>,
        spec: SliderSpec,
        keyLabel: (K) -> String,
        onEntryChange: (K, Float) -> Unit,
    ) {
        row()
        add(Label(title, basicUi.labelStyle)).left().colspan(3).padTop(12f)

        val liveMap = LinkedHashMap(map)
        val sumLabel = Label("", basicUi.labelStyle)

        fun refreshSum() {
            val sum = liveMap.percentageSum()
            val balanced = abs(sum - 100f) <= 0.05f
            sumLabel.style = if (balanced) basicUi.okLabelStyle else basicUi.errorLabelStyle
            sumLabel.setText("Total: %.1f%% %s".format(sum, if (balanced) "✓" else "(must total 100%)"))
        }

        map.forEach { (key, value) ->
            addSliderRow("  ${keyLabel(key)}", spec, value) { newValue ->
                liveMap[key] = newValue
                onEntryChange(key, newValue)
                refreshSum()
            }
        }

        row()
        add(sumLabel).left().colspan(3)
        refreshSum()
    }

    private fun <K> Table.addWeightMapSection(
        title: String,
        map: Map<K, Float>,
        spec: SliderSpec,
        keyLabel: (K) -> String,
        onEntryChange: (K, Float) -> Unit,
    ) {
        row()
        add(Label(title, basicUi.labelStyle)).left().colspan(3).padTop(12f)
        map.forEach { (key, value) ->
            addSliderRow("  ${keyLabel(key)}", spec, value) { newValue -> onEntryChange(key, newValue) }
        }
    }

    private fun Table.addSpeedSetRow(current: Set<SimSpeed>, onChangeValue: (Set<SimSpeed>) -> Unit) {
        row()
        add(Label("Enabled speed presets", basicUi.labelStyle)).left().colspan(3).padTop(12f)
        val liveSet = current.toMutableSet()
        SimSpeed.entries.forEach { speed ->
            row()
            val checkBox = CheckBox(" ${speed.label}", basicUi.checkBoxStyle)
            checkBox.isChecked = speed in liveSet
            checkBox.onChange {
                if (checkBox.isChecked) liveSet.add(speed) else liveSet.remove(speed)
                onChangeValue(liveSet.toSet())
            }
            add(checkBox).left().colspan(3)
        }
    }
}
