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

import org.jetbrains.dokka.model.doc.B
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.doc.Text

/**
 * A block tag of a documentation comment which holds a note on the API or on its
 * implementation, as introduced by the documentation of the JDK.
 *
 * Dokka parses these tags, but renders none of them.
 *
 * The constants follow the order in which the documentation of the JDK
 * shows the notes.
 *
 * @property tagName The name of the tag without the leading `@`.
 * @property title The title of the note in the rendered documentation.
 */
internal enum class NoteTag(val tagName: String, val title: String) {

    API_NOTE("apiNote", "API Note"),
    IMPL_SPEC("implSpec", "Implementation Requirements"),
    IMPL_NOTE("implNote", "Implementation Note");

    companion object {

        /**
         * Obtains the note tag with the given name.
         *
         * @return the tag, or `null` if there is no note tag with such a name
         */
        fun named(name: String): NoteTag? = entries.find { it.tagName == name }
    }
}

/**
 * The name of the root tag of a comment parsed by Dokka.
 *
 * Mirrors `MARKDOWN_ELEMENT_FILE_NAME` of the Kotlin analysis API of Dokka,
 * which is not on the compilation classpath of this module.
 */
private const val ROOT_TAG_NAME = "MARKDOWN_FILE"

/**
 * Moves the [note tags][NoteTag] of this documentation to the end of its description.
 *
 * The notes follow the order of the [NoteTag] constants. Notes of the same kind keep
 * the order of the comment. The first paragraph of a note starts with
 * the [title][NoteTag.title] of its tag, marked as bold. The HTML format shows the title
 * in bold, and the Javadoc format shows it as plain text. A note which starts with
 * another block, such as a list, gets its title in a paragraph of its own.
 *
 * If the documentation has no description, the notes make one.
 * Notes without content are dropped.
 *
 * @return the documentation with the notes in the description, or `null` if
 *   this documentation has no note tags
 */
internal fun DocumentationNode.withNotesInDescription(): DocumentationNode? {
    val notes = children.mapNotNull { it.asNote() }
    if (notes.isEmpty()) {
        return null
    }
    val others = children.filter { it.asNote() == null }
    val noteBlocks = notes
        .sortedBy { (tag, _) -> tag }
        .flatMap { (tag, note) -> note.toBlocks(tag) }
    return DocumentationNode(others.withBlocksInDescription(noteBlocks))
}

/**
 * Appends the given [blocks] to the description among these tags.
 *
 * Creates the description if there is none and there are blocks to append.
 */
private fun List<TagWrapper>.withBlocksInDescription(blocks: List<DocTag>): List<TagWrapper> {
    if (blocks.isEmpty()) {
        return this
    }
    val description = firstNotNullOfOrNull { it as? Description }
    val updated = description.appending(blocks)
    return if (description == null) {
        listOf(updated) + this
    } else {
        map { if (it === description) updated else it }
    }
}

/**
 * Returns this tag with its kind if this is a [note tag][NoteTag], or `null` otherwise.
 */
private fun TagWrapper.asNote(): Pair<NoteTag, CustomTagWrapper>? {
    val wrapper = this as? CustomTagWrapper ?: return null
    return NoteTag.named(wrapper.name)?.let { it to wrapper }
}

/**
 * Converts this note into blocks of the description, with the title of the [tag] first.
 */
private fun CustomTagWrapper.toBlocks(tag: NoteTag): List<DocTag> {
    val blocks = root.blocks
    val first = blocks.firstOrNull() ?: return emptyList()
    val title = B(listOf(Text("${tag.title}:")))
    return if (first is P) {
        val titled = first.copy(children = listOf(title, Text(" ")) + first.children)
        listOf(titled) + blocks.drop(1)
    } else {
        listOf(P(listOf(title))) + blocks
    }
}

/**
 * Creates a description with the content of this one, followed by the given [blocks].
 */
private fun Description?.appending(blocks: List<DocTag>): Description {
    val root = this?.root
    val newRoot = if (root is CustomDocTag && root.name == ROOT_TAG_NAME) {
        root.copy(children = root.children + blocks)
    } else {
        CustomDocTag(listOfNotNull(root) + blocks, name = ROOT_TAG_NAME)
    }
    return Description(newRoot)
}

/**
 * The top-level blocks of a comment, which Dokka wraps into a root tag.
 */
private val DocTag.blocks: List<DocTag>
    get() = if (this is CustomDocTag && name == ROOT_TAG_NAME) children else listOf(this)
