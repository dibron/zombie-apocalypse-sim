package com.zombiesim.core

import kotlinx.serialization.Serializable

@Serializable
enum class CitySize { Small, Medium, Large, Massive }

/** Broad zoning classification. Constrains which [BuildingType]s can spawn in it - see [DISTRICT_ALLOWED_BUILDINGS]. */
@Serializable
enum class DistrictType { Residential, Commercial, Industrial, Downtown, Government, Recreational }

/**
 * Specific, resource-bearing building kinds. Distinct from [DistrictType]: a district is a
 * zoning classification for world generation, a building is what Phase 3 actually places and
 * seeds with resources (e.g. Hospital -> medicine, PoliceStation -> weapons, Supermarket -> food).
 */
@Serializable
enum class BuildingType {
    Hospital, PoliceStation, Supermarket, MilitaryBase, Mall, Farm, Forest,
    MetroStation, GasStation, School, House, Apartment, Office, Factory, Warehouse,
    GovernmentBuilding, Park
}

/** Which [BuildingType]s Phase 3 world generation may place within each [DistrictType]. */
val DISTRICT_ALLOWED_BUILDINGS: Map<DistrictType, Set<BuildingType>> = mapOf(
    DistrictType.Residential to setOf(BuildingType.House, BuildingType.Apartment, BuildingType.School, BuildingType.Park),
    DistrictType.Commercial to setOf(BuildingType.Mall, BuildingType.Supermarket, BuildingType.Office, BuildingType.GasStation),
    DistrictType.Industrial to setOf(BuildingType.Factory, BuildingType.Warehouse, BuildingType.GasStation),
    DistrictType.Downtown to setOf(
        BuildingType.Mall, BuildingType.PoliceStation, BuildingType.Hospital, BuildingType.Office, BuildingType.MetroStation,
    ),
    DistrictType.Government to setOf(
        BuildingType.GovernmentBuilding, BuildingType.PoliceStation, BuildingType.Hospital,
        BuildingType.MetroStation, BuildingType.MilitaryBase,
    ),
    DistrictType.Recreational to setOf(BuildingType.Park, BuildingType.Forest, BuildingType.Farm, BuildingType.School),
)

@Serializable
enum class ZombieType { Walker, Runner, Crawler, Bloater, Screamer, Tank, Blind, Mutated }

@Serializable
enum class OutbreakLocation { Random, Hospital, Downtown, MilitaryBase, Custom }

// Note: no "Military" profession - military affiliation is a separate flag/component
// added in Phase 16 (Military AI). A Soldier can exist without being on active duty.
@Serializable
enum class Profession {
    Doctor, PoliceOfficer, Engineer, OfficeWorker, RetailWorker, Student, Retired,
    Mechanic, Electrician, Farmer, Chef, Soldier
}

@Serializable
enum class Personality {
    Cautious, Aggressive, Altruistic, Selfish, Leader, PanicProne, Stoic, Opportunist,
    // Distinct from Cautious (general risk-aversion): avoids violence even when directly threatened.
    Pacifist,
    // Biased toward always searching for/prioritizing loot over other goals.
    LootGoblin,
}

@Serializable
enum class MilitaryPresence { None, Weak, Standard, Overwhelming }

@Serializable
enum class Season { Spring, Summer, Autumn, Winter }

@Serializable
enum class SimulationLengthCap { Unlimited, OneMonth, OneYear, FiveYears }

@Serializable
enum class DifficultyPreset { Realistic, Survivable, Chaos, Custom }
