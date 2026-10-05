/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import io.spine.dependency.build.Dokka
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.local.Base

plugins {
    `detekt-code-analysis`
}

dependencies {
    // Only `io.spine.annotation.Internal` is used, and it lives in
    // `spine-annotations`. Depending on the whole `spine-base` put that
    // library into this plugin's runtime POM, where every consumer's Dokka
    // classpath then had to resolve it.
    implementation(Base.annotations)
    implementation(Dokka.BasePlugin.lib)

    compileOnly(Dokka.CorePlugin.lib)

    testImplementation(Dokka.CorePlugin.lib)

    // Dokka's test API runs Dokka on inline sources. These artifacts have no
    // declarations in `Dokka` because `buildSrc` comes from `config`.
    val dokka = "org.jetbrains.dokka"
    testImplementation("$dokka:dokka-test-api:${Dokka.version}")
    testImplementation("$dokka:dokka-base-test-utils:${Dokka.version}")
    // The K2 analysis, which the Dokka Gradle plugin uses by default.
    testRuntimeOnly("$dokka:analysis-kotlin-symbols:${Dokka.version}")
}

configurations.all {
    resolutionStrategy {
        // `dokka-base-test-utils` asks for the `kotlin-test` of the Kotlin
        // that Dokka is built with, older than the one forced for this project.
        force("${Kotlin.group}:kotlin-test:${Kotlin.runtimeVersion}")
    }
}
