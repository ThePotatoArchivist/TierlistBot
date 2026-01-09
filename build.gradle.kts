import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("application")
}

group = "archives.tater.tierlist"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("dev.kord:kord-core:${properties["kord_version"]}")
    implementation("io.github.cdimascio:dotenv-kotlin:${properties["dotenv_version"]}")
    implementation("org.slf4j:slf4j-simple:${properties["slf4j_version"]}")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    freeCompilerArgs.addAll(
        "-Xcontext-parameters",
        "-Xcontext-sensitive-resolution",
        "-Xexplicit-backing-fields"
    )
}

application {
    mainClass = "archives.tater.discordito.Main"
}
