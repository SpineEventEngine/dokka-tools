/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.JavaX
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.PublishingRepos.gitHub
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.license.LicenseReporter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    apply(from = "version.gradle.kts")

    val mcJavaVersion: String by extra

    io.spine.internal.gradle.doApplyStandard(repositories)

    dependencies {
        classpath("io.spine.tools:spine-mc-java:$mcJavaVersion")
    }
}

plugins {
    `java-library`
    kotlin("jvm")
    idea
    id("com.google.protobuf")
    id("net.ltgt.errorprone")
}

apply(from = "version.gradle.kts")
val spineCoreVersion: String by extra
val spineBaseVersion: String by extra
val spineTimeVersion: String by extra

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")
        apply(from = "$rootDir/version.gradle.kts")
    }

    group = "io.spine.template"
    version = extra["versionToPublish"]!!

    repositories.applyStandard()
}

spinePublishing {
    modules = subprojects.map { it.path }.toSet()
    destinations = setOf(
        PublishingRepos.cloudRepo,
        PublishingRepos.cloudArtifactRegistry,
        gitHub("dokka-tools")
    )
}

subprojects {
    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("com.google.protobuf")

        plugin("io.spine.mc-java")

        plugin("maven-publish")
        plugin("net.ltgt.errorprone")
        plugin("jacoco")
        plugin("pmd")
        plugin("pmd-settings")

        plugin<IncrementGuard>()
    }

    CheckStyleConfig.applyTo(project)
    LicenseReporter.generateReportIn(project)

    tasks.withType<JavaCompile> {
        configureJavac()
    }

    val javaVersion = 11
    kotlin {
        applyJvmToolchain(javaVersion)
        explicitApi()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
        setFreeCompilerArgs()
    }

    tasks.withType<JavaCompile> {
        configureJavac()
        configureErrorProne()
    }

    kotlin {
        explicitApi()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            freeCompilerArgs = listOf("-Xskip-prerelease-check")
        }
    }

    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(CheckerFramework.annotations)
        compileOnlyApi(JavaX.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        testImplementation(Guava.testLib)
        testImplementation(JUnit.runner)
        testImplementation(JUnit.pioneer)
        JUnit.api.forEach { testImplementation(it) }

        runtimeOnly(Flogger.Runtime.systemBackend)
        runtimeOnly(Flogger.Runtime.systemBackend)
    }

    configurations {
        all {
            resolutionStrategy {
                force(
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-testlib:$spineBaseVersion",
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion"
                )
            }
        }
    }

    configurations.forceVersions()
    configurations.excludeProtobufLite()

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
    }
}

// TODO: Apply after adding at least one Java subpropject.
// JavadocConfig.applyTo(project)
// PomGenerator.applyTo(project)
// LicenseReporter.mergeAllReports(project)