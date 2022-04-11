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
    implementation("org.jetbrains.dokka:dokka-base:${Dokka.version}")
}