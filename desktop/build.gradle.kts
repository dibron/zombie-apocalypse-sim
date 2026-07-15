plugins {
    kotlin("jvm") version "2.4.10"
    application
}

val gdxVersion = "1.13.1"

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.zombiesim.desktop.DesktopLauncherKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<JavaExec>("run") {
    // libGDX/LWJGL3 needs to run on the main thread on macOS; not needed elsewhere.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}
