plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
}

val gdxVersion = "1.13.1"
val fleksVersion = "2.14"
val kotlinxSerializationVersion = "1.11.0"

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("io.github.quillraven.fleks:Fleks:$fleksVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

kotlin {
    jvmToolchain(17)
}
