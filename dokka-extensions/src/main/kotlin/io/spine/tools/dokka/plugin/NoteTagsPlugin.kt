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

package io.spine.tools.dokka.plugin

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * Dokka plugin which renders the Javadoc block tags `@apiNote`, `@implSpec`,
 * and `@implNote`.
 *
 * Dokka keeps these tags in its model of the documentation but renders none of them,
 * neither in the HTML format nor in the Javadoc one. The plugin injects
 * the [NoteTagsTransformer], which puts the notes into the descriptions
 * of the documented elements.
 *
 * Dokka looks for [DokkaPlugin] subclasses on its classpath using [java.util.ServiceLoader].
 * Adding this module to the Dokka plugins of a project applies the plugin:
 * ```
 * dependencies {
 *      dokkaPlugin("io.spine.tools:dokka-extensions:${version}")
 * }
 * ```
 */
public class NoteTagsPlugin : DokkaPlugin() {

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    /**
     * A transformer registered at Dokka's extension point to put the notes into
     * descriptions before [org.jetbrains.dokka.model.Documentable]s from different
     * source sets are merged.
     */
    @Suppress("unused") // This delegated property has a desired side effect.
    public val noteTagsTransformer: Extension<PreMergeDocumentableTransformer, *, *>
            by extending {
                dokkaBase.preMergeDocumentableTransformer providing ::NoteTagsTransformer
            }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
