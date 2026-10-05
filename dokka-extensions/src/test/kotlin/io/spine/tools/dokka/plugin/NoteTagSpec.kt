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

import io.kotest.matchers.shouldBe
import io.spine.tools.dokka.plugin.NoteTag.API_NOTE
import io.spine.tools.dokka.plugin.NoteTag.IMPL_NOTE
import io.spine.tools.dokka.plugin.NoteTag.IMPL_SPEC
import org.jetbrains.dokka.model.doc.B
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Li
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Ul
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`NoteTag` should")
internal class NoteTagSpec {

    @Test
    fun `be found by the name of its tag`() {
        NoteTag.named("apiNote") shouldBe API_NOTE
        NoteTag.named("implSpec") shouldBe IMPL_SPEC
        NoteTag.named("implNote") shouldBe IMPL_NOTE
    }

    @Test
    fun `not be found by the name of another tag`() {
        NoteTag.named("since") shouldBe null
        NoteTag.named("ImplNote") shouldBe null
    }

    @Nested inner class
    `move notes to the description, so that` {

        @Test
        fun `documentation without notes is left as is`() {
            val docs = DocumentationNode(
                listOf(Description(root(paragraph("Packs messages."))), Since(root()))
            )
            docs.withNotesInDescription() shouldBe null
        }

        @Test
        fun `a note follows the description and starts with its title`() {
            val docs = DocumentationNode(
                listOf(
                    Description(root(paragraph("Packs messages."))),
                    note("implNote", paragraph("Uses a cache."))
                )
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    Description(
                        root(
                            paragraph("Packs messages."),
                            titled("Implementation Note", Text("Uses a cache."))
                        )
                    )
                )
            )
        }

        @Test
        fun `notes make the description if there is none`() {
            val docs = DocumentationNode(listOf(note("apiNote", paragraph("Prefer batches."))))
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(Description(root(titled("API Note", Text("Prefer batches.")))))
            )
        }

        @Test
        fun `a note starting with a list gets its title in a paragraph of its own`() {
            val list = Ul(listOf(Li(listOf(Text("Refuses to unpack.")))))
            val docs = DocumentationNode(
                listOf(Description(root(paragraph("Packs messages."))), note("implNote", list))
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    Description(
                        root(
                            paragraph("Packs messages."),
                            P(listOf(title("Implementation Note"))),
                            list
                        )
                    )
                )
            )
        }

        @Test
        fun `notes follow the order of the JDK, keeping the order of notes of a kind`() {
            val docs = DocumentationNode(
                listOf(
                    note("implNote", paragraph("First note.")),
                    note("apiNote", paragraph("API.")),
                    note("implNote", paragraph("Second note.")),
                    note("implSpec", paragraph("Requirements."))
                )
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    Description(
                        root(
                            titled("API Note", Text("API.")),
                            titled("Implementation Requirements", Text("Requirements.")),
                            titled("Implementation Note", Text("First note.")),
                            titled("Implementation Note", Text("Second note."))
                        )
                    )
                )
            )
        }

        @Test
        fun `roots other than that of a parsed comment serve as blocks`() {
            val docs = DocumentationNode(
                listOf(
                    Description(paragraph("Packs messages.")),
                    CustomTagWrapper(paragraph("Uses a cache."), "implNote")
                )
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    Description(
                        root(
                            paragraph("Packs messages."),
                            titled("Implementation Note", Text("Uses a cache."))
                        )
                    )
                )
            )
        }

        @Test
        fun `custom doc tags with other names serve as blocks`() {
            val descriptionRoot =
                CustomDocTag(listOf(paragraph("Packs messages.")), name = "custom")
            val noteRoot = CustomDocTag(listOf(paragraph("Uses a cache.")), name = "custom")
            val docs = DocumentationNode(
                listOf(Description(descriptionRoot), CustomTagWrapper(noteRoot, "implNote"))
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    Description(
                        root(descriptionRoot, P(listOf(title("Implementation Note"))), noteRoot)
                    )
                )
            )
        }

        @Test
        fun `other custom tags stay where they are`() {
            val suppress = CustomTagWrapper(root(), "suppress")
            val docs = DocumentationNode(
                listOf(
                    Description(root(paragraph("Packs messages."))),
                    suppress,
                    note("implNote", paragraph("Uses a cache."))
                )
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    Description(
                        root(
                            paragraph("Packs messages."),
                            titled("Implementation Note", Text("Uses a cache."))
                        )
                    ),
                    suppress
                )
            )
        }

        @Test
        fun `notes without content are dropped`() {
            val description = Description(root(paragraph("Packs messages.")))
            val docs = DocumentationNode(listOf(description, note("implNote")))
            docs.withNotesInDescription() shouldBe DocumentationNode(listOf(description))
        }

        @Test
        fun `other tags stay in their places`() {
            val param = Param(root(paragraph("The message.")), "message")
            val since = Since(root(paragraph("2.0.0")))
            val docs = DocumentationNode(
                listOf(
                    param,
                    Description(root(paragraph("Packs messages."))),
                    note("implNote", paragraph("Uses a cache.")),
                    since
                )
            )
            docs.withNotesInDescription() shouldBe DocumentationNode(
                listOf(
                    param,
                    Description(
                        root(
                            paragraph("Packs messages."),
                            titled("Implementation Note", Text("Uses a cache."))
                        )
                    ),
                    since
                )
            )
        }
    }
}

/**
 * Creates the root tag of a parsed comment with the given blocks.
 */
private fun root(vararg blocks: DocTag): DocTag =
    CustomDocTag(blocks.toList(), name = "MARKDOWN_FILE")

private fun paragraph(text: String): P = P(listOf(Text(text)))

/**
 * Creates a custom tag as Dokka parses an unknown block tag of a comment.
 */
private fun note(name: String, vararg blocks: DocTag): CustomTagWrapper =
    CustomTagWrapper(root(*blocks), name)

private fun title(text: String): B = B(listOf(Text("$text:")))

/**
 * Creates a paragraph which starts with the given title, as a note in a description does.
 */
private fun titled(text: String, vararg content: DocTag): P =
    P(listOf(title(text), Text(" ")) + content)
