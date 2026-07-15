plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
