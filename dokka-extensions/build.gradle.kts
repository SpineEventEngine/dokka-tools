import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.Spine

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    val spine = Spine(project)

    implementation(spine.base)
    implementation(Dokka.BasePlugin.lib)

    compileOnly(Dokka.CorePlugin.lib)
}
