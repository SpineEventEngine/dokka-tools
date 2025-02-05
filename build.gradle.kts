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

import io.spine.dependency.build.CheckerFramework
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.JavaX
import io.spine.dependency.lib.KotlinX
import io.spine.dependency.local.Logging
import io.spine.dependency.test.JUnit
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
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import io.spine.gradle.standardToSpineSdk
import io.spine.gradle.testing.configureLogging
import io.spine.gradle.testing.registerTestTasks
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    errorprone
    `gradle-doctor`
    idea
}

allprojects {
    apply {
        plugin("jacoco")
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
    destinations = setOf(
        PublishingRepos.cloudArtifactRegistry,
        gitHub("dokka-tools")
    )
}

subprojects {
    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("maven-publish")
        plugin("net.ltgt.errorprone")
        plugin("jacoco")
        plugin("pmd")
        plugin("pmd-settings")

        plugin<IncrementGuard>()
    }

    CheckStyleConfig.applyTo(project)
    LicenseReporter.generateReportIn(project)

    val javaVersion = JavaVersion.VERSION_11.toString()

    kotlin {
        applyJvmToolchain(javaVersion)
        explicitApi()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion
        setFreeCompilerArgs()
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
        testImplementation(JUnit.runner)
        JUnit.api.forEach { testImplementation(it) }
    }

    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                force(
                    Logging.lib,
                    KotlinX.Coroutines.core,
                    KotlinX.Coroutines.jdk8,
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

PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
