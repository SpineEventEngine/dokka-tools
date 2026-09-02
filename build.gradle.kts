/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.build.CheckerFramework
import io.spine.dependency.build.Dokka
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.kotlinx.Coroutines
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.JavaX
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.local.Base
import io.spine.dependency.local.Logging
import io.spine.dependency.test.JUnit
import io.spine.dependency.test.Kover
import io.spine.dependency.test.Kotest
import io.spine.gradle.checkstyle.CheckStyleConfig
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.kotlin.applyJvmToolchain
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.PublishingRepos.gitHub
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.report.coverage.KoverConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.testing.configureLogging
import io.spine.gradle.testing.registerTestTasks
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    kotlin("jvm")
    errorprone
    `gradle-doctor`
    idea
}

allprojects {
    apply {
        // Applies the Dokka plugin together with its Javadoc format.
        // Publishing depends on `dokkaGeneratePublicationJavadoc`, which only
        // the Javadoc format registers.
        plugin("dokka-setup")
        plugin("idea")
        plugin("project-report")
        from("$rootDir/version.gradle.kts")
    }

    group = "io.spine.tools"
    version = extra["versionToPublish"]!!

    repositories.standardToSpineSdk()
}

spinePublishing {
    modules = subprojects.map { it.path }.toSet()
    // No prefix, as for the other tool artifacts. This renames the published
    // coordinate to `io.spine.tools:dokka-extensions`; consumers reach it
    // through `Dokka.SpineExtensions` in `config`.
    toolArtifactPrefix = "NONE"
    destinations = setOf(
        PublishingRepos.cloudArtifactRegistry,
        gitHub("dokka-tools")
    )
}

subprojects {
    apply {
        plugin("java-library")
        plugin("kotlin")
        // Coverage reaches the root rollup only from subprojects that apply
        // Kover themselves; this repository does not use the `jvm-module`
        // convention, which would otherwise do it.
        plugin(Kover.id)
        plugin("maven-publish")
        plugin("net.ltgt.errorprone")
        plugin("pmd")
        plugin("pmd-settings")

        plugin<IncrementGuard>()
    }

    CheckStyleConfig.applyTo(project)
    LicenseReporter.generateReportIn(project)

    kotlin {
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
        applyJvmToolchain(BuildSettings.javaVersion.asInt())
        explicitApi()
    }

    tasks.withType<JavaCompile> {
        configureJavac()
        configureErrorProne()
    }

    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(CheckerFramework.annotations)
        compileOnlyApi(JavaX.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        testImplementation(Guava.testLib)
        testImplementation(Kotest.assertions)
        testImplementation(JUnit.Jupiter.engine)
        JUnit.Jupiter.modules.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.Platform.launcher)
    }

    apply<BomsPlugin>()

    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                // Dokka brings its own Kotlin runtime, older than the one
                // this project compiles against, and `failOnVersionConflict()`
                // cannot choose between them.
                Kotlin.StdLib.forceArtifacts(project, this@all, this@resolutionStrategy)
                force(
                    // The published `dokka-extensions` this project loads for
                    // its own documentation still asks for a `spine-base` that
                    // is no longer in the registry. Superseded once the fixed
                    // plugin is published.
                    Base.lib,
                    Kotlin.bom,
                    // Dokka requests an older coroutines line than the
                    // refreshed baseline; the BOM itself is a graph node
                    // that `failOnVersionConflict()` trips on.
                    Coroutines.bom,
                    Kotlin.run { artifact(reflect) },
                    Logging.lib,
                )
                Coroutines.forceArtifacts(
                    project, this@all, this@resolutionStrategy
                )
            }
        }
    }

    tasks {
        registerTestTasks()
        withType<Test>().configureEach {
            configureLogging()
            useJUnitPlatform()
        }
    }
}

// The root project documents itself too, so it loads `dokka-extensions` and
// needs the same repair as the subprojects: the published plugin still asks
// for a `spine-base` the registry no longer serves. `subprojects` does not
// reach the root, so the force is repeated here.
configurations.all {
    resolutionStrategy {
        force(Base.lib)
    }
}

// Kover must be applied while the project is still configurable.
KoverConfig.applyTo(project)

PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
