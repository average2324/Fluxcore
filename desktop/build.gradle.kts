plugins {
    application
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-desktop")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.luminadigitale.fluxcore.desktop.DesktopLauncherKt")
}

tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.file("assets")
}

val smokeRun by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs the desktop app briefly to verify startup/render/exit path."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(application.mainClass)
    args("--smoke-seconds=3")
    workingDir = rootProject.file("assets")
}

val smallScreenRun by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Runs the desktop app in a compact phone-sized window for manual UI testing."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(application.mainClass)
    args("--width=390", "--height=844")
    workingDir = rootProject.file("assets")
}

val shipPreview by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Renders all 11 procedural ships into ship_preview.png for visual review."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.luminadigitale.fluxcore.desktop.DesktopShipPreviewKt")
    args(rootProject.file("ship_preview.png").absolutePath)
    workingDir = rootProject.file("assets")
}

tasks.test {
    useJUnitPlatform()
}

tasks.clean {
    delete(layout.projectDirectory.dir("bin"))
}
