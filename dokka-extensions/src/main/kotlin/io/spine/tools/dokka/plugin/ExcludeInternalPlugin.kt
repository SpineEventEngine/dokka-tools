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

package io.spine.tools.dokka.plugin

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * Dokka plugin which excludes code annotated with [io.spine.annotation.Internal]. Works for
 * types, methods and fields.
 *
 * There are several extensions points at different stages of generating documentation. The plugin
 * below injects the [ExcludeInternalTransformer] at the stage when the source code of a project is
 * already collected and translated to the Dokka internal representation
 * [org.jetbrains.dokka.model.Documentable].
 *
 * Dokka looks for [DokkaPlugin] subclasses on its classpath during setup using
 * [java.util.ServiceLoader]. The way you use this plugin is provided below:
 * ```
 * dependencies {
 *      dokkaPlugin("io.spine.tools:spine-dokka-extensions:${version}")
 * }
 * ```
 *
 * @see <a href="https://kotlin.github.io/dokka/1.6.10/developer_guide/introduction/">
 *     Guide to Dokka Plugin development</a>
 *
 * @see <a href="https://kotlin.github.io/dokka/1.6.10/developer_guide/extension_points/#pre-merge-documentation-transform">
 *     Pre-merge documentation transform</a>
 */
class ExcludeInternalPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    /**
     * A transformer registered in Dokka's extension point to exclude [io.spine.annotation.Internal]
     * before [org.jetbrains.dokka.model.Documentable]s from different source sets are merged.
     */
    val excludeInternalTransformer: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::ExcludeInternalTransformer
    }
}
