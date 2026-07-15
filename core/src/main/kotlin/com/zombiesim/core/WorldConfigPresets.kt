package com.zombiesim.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Saves/loads named [WorldConfig] presets as JSON files in a `presets/` folder next to the working directory. */
class WorldConfigPresets(private val presetsDir: File = File("presets")) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun listPresetNames(): List<String> =
        presetsDir.takeIf { it.isDirectory }
            ?.listFiles { file -> file.isFile && file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()

    fun save(name: String, config: WorldConfig) {
        presetsDir.mkdirs()
        File(presetsDir, "${name.sanitizedFileName()}.json").writeText(json.encodeToString(config))
    }

    fun load(name: String): WorldConfig? {
        val file = File(presetsDir, "${name.sanitizedFileName()}.json")
        if (!file.isFile) return null
        return json.decodeFromString(WorldConfig.serializer(), file.readText())
    }

    private fun String.sanitizedFileName(): String = replace(Regex("[^A-Za-z0-9_-]"), "_")
}
