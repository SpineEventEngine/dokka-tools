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

    compileOnly(Dokka.CorePlugin.lib)
    implementation(Dokka.BasePlugin.lib)
}