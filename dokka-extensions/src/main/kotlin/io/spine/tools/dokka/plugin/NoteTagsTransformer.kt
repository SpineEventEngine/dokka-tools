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

import org.jetbrains.dokka.base.transformers.documentables.DocumentableReplacerTransformer
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Renders the Javadoc block tags `@apiNote`, `@implSpec`, and `@implNote` by moving
 * them to the end of the description of each documented element.
 *
 * Dokka parses such tags as custom ones. The HTML format shows a custom tag only if
 * a content provider is registered for it, and the Javadoc format does not show custom
 * tags at all. Both formats show the description, so the notes are put there. Each note
 * starts with its title, such as "Implementation Note:". See [NoteTag] for the titles.
 *
 * The brief of an element stays as it is, with two exceptions. The HTML format takes
 * the first sentence of a Java description or the first paragraph of a KDoc one as
 * the brief, and the Javadoc format takes the first sentence of either. So:
 *  1. If an element has notes but no description, the brief is the beginning of
 *     the first note, title included.
 *  2. If a description has no sentence terminator where the first sentence is taken,
 *     the brief runs on into the first note.
 *
 * The transformer works before the documentables of different source sets are merged,
 * so each source set of an element gets its notes from its own documentation.
 *
 * @param context The context of the Dokka run.
 */
public class NoteTagsTransformer(context: DokkaContext) :
    DocumentableReplacerTransformer(context) {

    override fun processModule(module: DModule): AnyWithChanges<DModule> =
        super.processModule(module).withNotes { copy(documentation = it) }

    override fun processPackage(dPackage: DPackage): AnyWithChanges<DPackage> =
        super.processPackage(dPackage).withNotes { copy(documentation = it) }

    override fun processClassLike(classlike: DClasslike): AnyWithChanges<DClasslike> =
        super.processClassLike(classlike).withNotes { documentation ->
            when (this) {
                is DClass -> copy(documentation = documentation)
                is DInterface -> copy(documentation = documentation)
                is DObject -> copy(documentation = documentation)
                is DAnnotation -> copy(documentation = documentation)
                is DEnum -> copy(documentation = documentation)
            }
        }

    override fun processEnumEntry(dEnumEntry: DEnumEntry): AnyWithChanges<DEnumEntry> =
        super.processEnumEntry(dEnumEntry).withNotes { copy(documentation = it) }

    override fun processFunction(dFunction: DFunction): AnyWithChanges<DFunction> =
        super.processFunction(dFunction).withNotes { copy(documentation = it) }

    override fun processProperty(dProperty: DProperty): AnyWithChanges<DProperty> =
        super.processProperty(dProperty).withNotes { copy(documentation = it) }

    override fun processParameter(dParameter: DParameter): AnyWithChanges<DParameter> =
        super.processParameter(dParameter).withNotes { copy(documentation = it) }

    override fun processTypeParameter(
        dTypeParameter: DTypeParameter
    ): AnyWithChanges<DTypeParameter> =
        super.processTypeParameter(dTypeParameter).withNotes { copy(documentation = it) }

    override fun processTypeAlias(dTypeAlias: DTypeAlias): AnyWithChanges<DTypeAlias> =
        super.processTypeAlias(dTypeAlias).withNotes { copy(documentation = it) }

    /**
     * Moves the notes of the processed documentable to its description.
     *
     * Returns this result as is if the documentable has no notes.
     *
     * @param copy Creates a copy of the documentable with the given documentation.
     */
    private fun <T : Documentable> AnyWithChanges<T>.withNotes(
        copy: T.(SourceSetDependent<DocumentationNode>) -> T
    ): AnyWithChanges<T> {
        val documentable = target ?: return this
        val documentation = documentable.documentation.withNotesInDescription()
        return documentation?.let { AnyWithChanges(documentable.copy(it), changed = true) } ?: this
    }
}

/**
 * Moves the note tags to the description in each source set of this documentation.
 *
 * @return the updated documentation, or `null` if no source set has note tags
 */
private fun SourceSetDependent<DocumentationNode>.withNotesInDescription():
        SourceSetDependent<DocumentationNode>? {
    val updated = mapValues { (_, node) -> node.withNotesInDescription() }
    if (updated.values.all { it == null }) {
        return null
    }
    return mapValues { (sourceSet, node) -> updated[sourceSet] ?: node }
}
