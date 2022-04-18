import io.spine.internal.dependency.Dokka

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val spineBaseVersion: String by project

dependencies {
    implementation("io.spine:spine-base:${spineBaseVersion}")

    compileOnly("org.jetbrains.dokka:dokka-core:${Dokka.version}")
    implementation(Dokka.BasePlugin.lib)
}