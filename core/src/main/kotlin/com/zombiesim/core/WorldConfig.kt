package com.zombiesim.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.random.Random

/** Percentage-style maps (district mix, profession/personality distribution) should sum to 100. */
fun <K> Map<K, Float>.percentageSum(): Float = values.sum()

fun <K> Map<K, Float>.isBalancedPercentage(tolerance: Float = 0.05f): Boolean =
    abs(percentageSum() - 100f) <= tolerance

private fun <T> normalizedRandomWeights(keys: List<T>, random: Random, total: Float = 100f): Map<T, Float> {
    val rolls = keys.associateWith { 1f + random.nextFloat() * 99f }
    val rollTotal = rolls.values.sum()
    return rolls.mapValues { (_, v) -> (v / rollTotal) * total }
}

/**
 * Full pre-simulation configuration, grouped by category to match the setup screen's tabs.
 * Every numeric field has a matching [SliderSpec] constant in its category's companion object,
 * which is the single source of truth for both the default value and the setup screen's slider
 * range - there is no separate "UI config" to keep in sync.
 */
@Serializable
data class WorldConfig(
    val cityMap: CityMap = CityMap(),
    val infection: Infection = Infection(),
    val population: Population = Population(),
    val economy: Economy = Economy(),
    val military: Military = Military(),
    val weather: Weather = Weather(),
    val noiseAndCombat: NoiseAndCombat = NoiseAndCombat(),
    val meta: Meta = Meta(),
) {
    @Serializable
    data class CityMap(
        val citySize: CitySize = CitySize.Medium,
        val districtDensity: Float = DISTRICT_DENSITY.default,
        val districtMix: Map<DistrictType, Float> = defaultDistrictMix(),
        val numExits: Int = NUM_EXITS.default.toInt(),
        val metroEnabled: Boolean = true,
        val startingPopulation: Int = STARTING_POPULATION.default.toInt(),
        val mapSeed: String = "",
    ) {
        companion object {
            val DISTRICT_DENSITY = SliderSpec(0f, 1f, 0.6f, 0.01f)
            val NUM_EXITS = SliderSpec(0f, 8f, 3f, 1f)
            val STARTING_POPULATION = SliderSpec(500f, 100_000f, 5_000f, 100f)
            val DISTRICT_PERCENTAGE = SliderSpec(0f, 100f, 0f, 1f)

            fun defaultDistrictMix(): Map<DistrictType, Float> = linkedMapOf(
                DistrictType.Residential to 40f,
                DistrictType.Commercial to 20f,
                DistrictType.Industrial to 15f,
                DistrictType.Downtown to 10f,
                DistrictType.Government to 5f,
                DistrictType.Recreational to 10f,
            )

            fun random(random: Random): CityMap = CityMap(
                citySize = CitySize.entries.random(random),
                districtDensity = DISTRICT_DENSITY.randomValue(random),
                districtMix = normalizedRandomWeights(DistrictType.entries, random),
                numExits = NUM_EXITS.randomValue(random).toInt(),
                metroEnabled = random.nextBoolean(),
                startingPopulation = STARTING_POPULATION.randomValue(random).toInt(),
                mapSeed = random.nextLong().toString(),
            )
        }
    }

    @Serializable
    data class Infection(
        val reanimateOnAnyDeath: Boolean = true,
        val incubationMinHours: Int = INCUBATION_MIN_HOURS.default.toInt(),
        val incubationMaxHours: Int = INCUBATION_MAX_HOURS.default.toInt(),
        val biteLethality: Float = BITE_LETHALITY.default,
        val zombieBaseSpeed: Float = ZOMBIE_BASE_SPEED.default,
        val zombieDecayRate: Float = ZOMBIE_DECAY_RATE.default,
        val mutationEnabled: Boolean = true,
        val mutationSpeed: Float = MUTATION_SPEED.default,
        val airborneMutationPossible: Boolean = false,
        val zombieTypeSpawnWeights: Map<ZombieType, Float> = defaultZombieTypeWeights(),
        val startingOutbreakSize: Float = STARTING_OUTBREAK_SIZE.default,
        val startingOutbreakLocation: OutbreakLocation = OutbreakLocation.Random,
    ) {
        companion object {
            val INCUBATION_MIN_HOURS = SliderSpec(1f, 168f, 8f, 1f)
            val INCUBATION_MAX_HOURS = SliderSpec(1f, 168f, 72f, 1f)
            val BITE_LETHALITY = SliderSpec(0f, 1f, 0.9f, 0.01f)
            val ZOMBIE_BASE_SPEED = SliderSpec(0f, 1f, 0.4f, 0.01f)
            val ZOMBIE_DECAY_RATE = SliderSpec(0f, 1f, 0.2f, 0.01f)
            val MUTATION_SPEED = SliderSpec(0f, 1f, 0.3f, 0.01f)
            val STARTING_OUTBREAK_SIZE = SliderSpec(0f, 1f, 0.02f, 0.01f)
            val ZOMBIE_WEIGHT = SliderSpec(0f, 100f, 0f, 1f)

            fun defaultZombieTypeWeights(): Map<ZombieType, Float> = linkedMapOf(
                ZombieType.Walker to 40f,
                ZombieType.Runner to 15f,
                ZombieType.Crawler to 10f,
                ZombieType.Bloater to 8f,
                ZombieType.Screamer to 10f,
                ZombieType.Tank to 7f,
                ZombieType.Blind to 5f,
                ZombieType.Mutated to 5f,
            )

            fun random(random: Random): Infection {
                val minH = INCUBATION_MIN_HOURS.randomValue(random).toInt()
                val maxH = maxOf(minH, INCUBATION_MAX_HOURS.randomValue(random).toInt())
                return Infection(
                    reanimateOnAnyDeath = random.nextBoolean(),
                    incubationMinHours = minH,
                    incubationMaxHours = maxH,
                    biteLethality = BITE_LETHALITY.randomValue(random),
                    zombieBaseSpeed = ZOMBIE_BASE_SPEED.randomValue(random),
                    zombieDecayRate = ZOMBIE_DECAY_RATE.randomValue(random),
                    mutationEnabled = random.nextBoolean(),
                    mutationSpeed = MUTATION_SPEED.randomValue(random),
                    airborneMutationPossible = random.nextBoolean(),
                    zombieTypeSpawnWeights = normalizedRandomWeights(ZombieType.entries, random),
                    startingOutbreakSize = STARTING_OUTBREAK_SIZE.randomValue(random),
                    startingOutbreakLocation = OutbreakLocation.entries.random(random),
                )
            }
        }
    }

    @Serializable
    data class Population(
        val professionDistribution: Map<Profession, Float> = defaultProfessionDistribution(),
        val personalityDistribution: Map<Personality, Float> = defaultPersonalityDistribution(),
        val averageAge: Int = AVERAGE_AGE.default.toInt(),
        val ageSpread: Int = AGE_SPREAD.default.toInt(),
        val startingMorale: Float = STARTING_MORALE.default,
        val startingFearSensitivity: Float = STARTING_FEAR_SENSITIVITY.default,
        val loyaltyBaseline: Float = LOYALTY_BASELINE.default,
        val socialNetworkDensity: Float = SOCIAL_NETWORK_DENSITY.default,
        val weaponOwnershipRate: Float = WEAPON_OWNERSHIP_RATE.default,
    ) {
        companion object {
            val AVERAGE_AGE = SliderSpec(5f, 90f, 38f, 1f)
            val AGE_SPREAD = SliderSpec(1f, 40f, 15f, 1f)
            val STARTING_MORALE = SliderSpec(0f, 1f, 0.6f, 0.01f)
            val STARTING_FEAR_SENSITIVITY = SliderSpec(0f, 1f, 0.5f, 0.01f)
            val LOYALTY_BASELINE = SliderSpec(0f, 1f, 0.5f, 0.01f)
            val SOCIAL_NETWORK_DENSITY = SliderSpec(0f, 1f, 0.4f, 0.01f)
            val WEAPON_OWNERSHIP_RATE = SliderSpec(0f, 1f, 0.15f, 0.01f)
            val DISTRIBUTION_PERCENTAGE = SliderSpec(0f, 100f, 0f, 1f)

            fun defaultProfessionDistribution(): Map<Profession, Float> = linkedMapOf(
                Profession.Doctor to 3f,
                Profession.PoliceOfficer to 2f,
                Profession.Engineer to 6f,
                Profession.OfficeWorker to 24f,
                Profession.RetailWorker to 12f,
                Profession.Student to 18f,
                Profession.Retired to 20f,
                Profession.Mechanic to 4f,
                Profession.Electrician to 3f,
                Profession.Farmer to 3f,
                Profession.Chef to 3f,
                Profession.Soldier to 2f,
            )

            fun defaultPersonalityDistribution(): Map<Personality, Float> = linkedMapOf(
                Personality.Cautious to 15f,
                Personality.Aggressive to 8f,
                Personality.Altruistic to 12f,
                Personality.Selfish to 8f,
                Personality.Leader to 5f,
                Personality.PanicProne to 12f,
                Personality.Stoic to 12f,
                Personality.Opportunist to 8f,
                Personality.Pacifist to 10f,
                Personality.LootGoblin to 10f,
            )

            fun random(random: Random): Population {
                val age = AVERAGE_AGE.randomValue(random).toInt()
                return Population(
                    professionDistribution = normalizedRandomWeights(Profession.entries, random),
                    personalityDistribution = normalizedRandomWeights(Personality.entries, random),
                    averageAge = age,
                    ageSpread = AGE_SPREAD.randomValue(random).toInt(),
                    startingMorale = STARTING_MORALE.randomValue(random),
                    startingFearSensitivity = STARTING_FEAR_SENSITIVITY.randomValue(random),
                    loyaltyBaseline = LOYALTY_BASELINE.randomValue(random),
                    socialNetworkDensity = SOCIAL_NETWORK_DENSITY.randomValue(random),
                    weaponOwnershipRate = WEAPON_OWNERSHIP_RATE.randomValue(random),
                )
            }
        }
    }

    @Serializable
    data class ResourceAbundance(
        val food: Float = 0.6f,
        val water: Float = 0.6f,
        val medicine: Float = 0.5f,
        val fuel: Float = 0.5f,
        val ammo: Float = 0.3f,
    ) {
        companion object {
            val SPEC = SliderSpec(0f, 1f, 0.5f, 0.01f)

            fun random(random: Random): ResourceAbundance = ResourceAbundance(
                food = SPEC.randomValue(random),
                water = SPEC.randomValue(random),
                medicine = SPEC.randomValue(random),
                fuel = SPEC.randomValue(random),
                ammo = SPEC.randomValue(random),
            )
        }
    }

    @Serializable
    data class Economy(
        val startingAbundance: ResourceAbundance = ResourceAbundance(),
        val consumptionRate: Float = CONSUMPTION_RATE.default,
        val tradingEnabled: Boolean = true,
        val scarcityViolenceThreshold: Float = SCARCITY_VIOLENCE_THRESHOLD.default,
        val productionEfficiency: Float = PRODUCTION_EFFICIENCY.default,
    ) {
        companion object {
            val CONSUMPTION_RATE = SliderSpec(0f, 1f, 0.4f, 0.01f)
            val SCARCITY_VIOLENCE_THRESHOLD = SliderSpec(0f, 1f, 0.3f, 0.01f)
            val PRODUCTION_EFFICIENCY = SliderSpec(0f, 1f, 0.5f, 0.01f)

            fun random(random: Random): Economy = Economy(
                startingAbundance = ResourceAbundance.random(random),
                consumptionRate = CONSUMPTION_RATE.randomValue(random),
                tradingEnabled = random.nextBoolean(),
                scarcityViolenceThreshold = SCARCITY_VIOLENCE_THRESHOLD.randomValue(random),
                productionEfficiency = PRODUCTION_EFFICIENCY.randomValue(random),
            )
        }
    }

    @Serializable
    data class Military(
        val presenceLevel: MilitaryPresence = MilitaryPresence.Standard,
        val responseDelayHours: Int = RESPONSE_DELAY_HOURS.default.toInt(),
        val stageAggression: Float = STAGE_AGGRESSION.default,
        val policeCompetence: Float = POLICE_COMPETENCE.default,
        val evacuationProtocolEnabled: Boolean = true,
    ) {
        companion object {
            val RESPONSE_DELAY_HOURS = SliderSpec(0f, 168f, 12f, 1f)
            val STAGE_AGGRESSION = SliderSpec(0f, 1f, 0.5f, 0.01f)
            val POLICE_COMPETENCE = SliderSpec(0f, 1f, 0.5f, 0.01f)

            fun random(random: Random): Military = Military(
                presenceLevel = MilitaryPresence.entries.random(random),
                responseDelayHours = RESPONSE_DELAY_HOURS.randomValue(random).toInt(),
                stageAggression = STAGE_AGGRESSION.randomValue(random),
                policeCompetence = POLICE_COMPETENCE.randomValue(random),
                evacuationProtocolEnabled = random.nextBoolean(),
            )
        }
    }

    @Serializable
    data class Weather(
        val startingSeason: Season = Season.Spring,
        val weatherVolatility: Float = WEATHER_VOLATILITY.default,
        val winterSeverity: Float = WINTER_SEVERITY.default,
        val disasterEventsEnabled: Boolean = true,
    ) {
        companion object {
            val WEATHER_VOLATILITY = SliderSpec(0f, 1f, 0.4f, 0.01f)
            val WINTER_SEVERITY = SliderSpec(0f, 1f, 0.5f, 0.01f)

            fun random(random: Random): Weather = Weather(
                startingSeason = Season.entries.random(random),
                weatherVolatility = WEATHER_VOLATILITY.randomValue(random),
                winterSeverity = WINTER_SEVERITY.randomValue(random),
                disasterEventsEnabled = random.nextBoolean(),
            )
        }
    }

    @Serializable
    data class NoiseAndCombat(
        val noisePropagationRange: Float = NOISE_PROPAGATION_RANGE.default,
        val gunfireAttractionMultiplier: Float = GUNFIRE_ATTRACTION_MULTIPLIER.default,
        val barricadeEffectiveness: Float = BARRICADE_EFFECTIVENESS.default,
        val combatLethality: Float = COMBAT_LETHALITY.default,
        val friendlyFireEnabled: Boolean = true,
    ) {
        companion object {
            val NOISE_PROPAGATION_RANGE = SliderSpec(0f, 1f, 0.5f, 0.01f)
            val GUNFIRE_ATTRACTION_MULTIPLIER = SliderSpec(0f, 1f, 0.7f, 0.01f)
            val BARRICADE_EFFECTIVENESS = SliderSpec(0f, 1f, 0.5f, 0.01f)
            val COMBAT_LETHALITY = SliderSpec(0f, 1f, 0.5f, 0.01f)

            fun random(random: Random): NoiseAndCombat = NoiseAndCombat(
                noisePropagationRange = NOISE_PROPAGATION_RANGE.randomValue(random),
                gunfireAttractionMultiplier = GUNFIRE_ATTRACTION_MULTIPLIER.randomValue(random),
                barricadeEffectiveness = BARRICADE_EFFECTIVENESS.randomValue(random),
                combatLethality = COMBAT_LETHALITY.randomValue(random),
                friendlyFireEnabled = random.nextBoolean(),
            )
        }
    }

    @Serializable
    data class Meta(
        val simulationLengthCap: SimulationLengthCap = SimulationLengthCap.Unlimited,
        val enabledSpeedPresets: Set<SimSpeed> = SimSpeed.entries.toSet(),
        val randomEventFrequency: Float = RANDOM_EVENT_FREQUENCY.default,
        val difficultyPreset: DifficultyPreset = DifficultyPreset.Realistic,
    ) {
        companion object {
            val RANDOM_EVENT_FREQUENCY = SliderSpec(0f, 1f, 0.4f, 0.01f)

            fun random(random: Random): Meta = Meta(
                simulationLengthCap = SimulationLengthCap.entries.random(random),
                enabledSpeedPresets = SimSpeed.entries.toSet(),
                randomEventFrequency = RANDOM_EVENT_FREQUENCY.randomValue(random),
                difficultyPreset = DifficultyPreset.entries.random(random),
            )
        }
    }

    companion object {
        fun default(): WorldConfig = WorldConfig()

        fun randomized(random: Random = Random.Default): WorldConfig = WorldConfig(
            cityMap = CityMap.random(random),
            infection = Infection.random(random),
            population = Population.random(random),
            economy = Economy.random(random),
            military = Military.random(random),
            weather = Weather.random(random),
            noiseAndCombat = NoiseAndCombat.random(random),
            meta = Meta.random(random),
        )
    }
}
