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

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.util.ServiceLoader
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.translators.documentables.firstSentenceBriefFromContentNodes
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentHeader
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

/**
 * Runs Dokka on inline sources with the plugins found on the classpath.
 *
 * [NoteTagsPlugin] comes in through [ServiceLoader], as it does for the users
 * of this module. The tests render the HTML format.
 */
@DisplayName("`NoteTagsPlugin` should")
internal class NoteTagsPluginSpec :
    BaseAbstractTest(TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))) {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java", "src/main/kotlin")
            }
        }
    }

    @Test
    fun `be loadable as a service`() {
        val loader = ServiceLoader.load(DokkaPlugin::class.java)
        loader.find { it is NoteTagsPlugin } shouldNotBe null
    }

    @Test
    fun `render a note of a Java class`() {
        val writer = TestOutputWriterPlugin()
        testInline(PACKER_SOURCE, configuration, pluginOverrides = listOf(writer)) {
            documentablesTransformationStage = { module ->
                val packer = module.classlike("Packer")
                packer.documentation.values.single().children
                    .filterIsInstance<CustomTagWrapper>()
                    .shouldBeEmpty()
                packer.descriptionBlocks() shouldContainExactly listOf(
                    "Packs messages into envelopes.",
                    "Use it for messages of any type.",
                    "Implementation Note: Packing is delegated to add, which keeps the message.",
                    "An envelope refuses to unpack into another type."
                )
            }
            renderingStage = { _, _ ->
                val page = writer.page("root/sample/-packer/index.html")
                page shouldContain "<strong>Implementation Note:</strong>"
                page shouldContain "refuses to unpack into another type"
            }
        }
    }

    @Test
    fun `keep the first sentence of a Java description as the brief`() {
        val writer = TestOutputWriterPlugin()
        testInline(PACKER_SOURCE, configuration, pluginOverrides = listOf(writer)) {
            documentablesTransformationStage = { module ->
                module.classlike("Packer").brief() shouldBe "Packs messages into envelopes."
            }
            renderingStage = { _, _ ->
                val packagePage = writer.page("root/sample/index.html")
                packagePage shouldContain "Packs messages into envelopes."
                packagePage shouldNotContain "Implementation Note"
            }
        }
    }

    /**
     * Checks the content which the Javadoc format renders for a description.
     *
     * The Javadoc format drops headers and renders paragraphs, so a note must not
     * depend on anything else.
     */
    @Test
    fun `keep a note in paragraphs for the Javadoc format`() {
        testInline(PACKER_SOURCE, configuration) {
            documentablesTransformationStage = { module ->
                val content = module.classlike("Packer").descriptionContent()
                    .flatMap { it.withDescendants().toList() }
                content.filterIsInstance<ContentHeader>().shouldBeEmpty()
                content.filterIsInstance<ContentGroup>()
                    .filter { TextStyle.Paragraph in it.style }
                    .flatMap { it.children }
                    .filterIsInstance<ContentText>()
                    .map { it.text } shouldContain "Implementation Note:"
            }
        }
    }

    @Test
    fun `render notes of a Java interface, enum, field, and annotation`() {
        testInline(
            """
            |/src/main/java/sample/Codec.java
            |package sample;
            |
            |/**
            | * Encodes values.
            | *
            | * @apiNote Implementations are stateless.
            | */
            |public interface Codec {
            |
            |    /**
            |     * Encodes the value.
            |     *
            |     * @implSpec Calls {@code toString()}.
            |     */
            |    default String encode(Object value) {
            |        return value.toString();
            |    }
            |}
            |/src/main/java/sample/Mode.java
            |package sample;
            |
            |/**
            | * Modes of packing.
            | *
            | * @apiNote Prefer the fast one.
            | */
            |public enum Mode {
            |
            |    /**
            |     * Packs fast.
            |     *
            |     * @implNote Skips validation.
            |     */
            |    FAST;
            |
            |    /**
            |     * The size of a buffer.
            |     *
            |     * @implNote Fits a page.
            |     */
            |    public static final int BUFFER = 4096;
            |}
            |/src/main/java/sample/Packed.java
            |package sample;
            |
            |/**
            | * Marks packed types.
            | *
            | * @apiNote Applies to classes only.
            | */
            |public @interface Packed {
            |}
            """,
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val codec = module.classlike("Codec")
                codec.descriptionBlocks() shouldContainExactly listOf(
                    "Encodes values.",
                    "API Note: Implementations are stateless."
                )
                codec.functions.single { it.name == "encode" }
                    .descriptionBlocks() shouldContainExactly listOf(
                        "Encodes the value.",
                        "Implementation Requirements: Calls toString()."
                    )
                val mode = module.classlike("Mode") as DEnum
                mode.descriptionBlocks() shouldContainExactly listOf(
                    "Modes of packing.",
                    "API Note: Prefer the fast one."
                )
                mode.entries.single().descriptionBlocks() shouldContainExactly listOf(
                    "Packs fast.",
                    "Implementation Note: Skips validation."
                )
                mode.properties.single { it.name == "BUFFER" }
                    .descriptionBlocks() shouldContainExactly listOf(
                        "The size of a buffer.",
                        "Implementation Note: Fits a page."
                    )
                module.classlike("Packed").descriptionBlocks() shouldContainExactly listOf(
                    "Marks packed types.",
                    "API Note: Applies to classes only."
                )
            }
        }
    }

    @Test
    fun `render notes of a Kotlin object, property, and type alias`() {
        testInline(
            """
            |/src/main/kotlin/sample/Registry.kt
            |package sample
            |
            |/**
            | * Keeps the codecs.
            | *
            | * @implNote Not thread-safe.
            | */
            |public object Registry {
            |
            |    /**
            |     * The number of codecs.
            |     *
            |     * @apiNote Grows as codecs register.
            |     */
            |    public val size: Int = 0
            |}
            |
            |/**
            | * Names of codecs.
            | *
            | * @implNote Kept sorted.
            | */
            |public typealias Names = List<String>
            """,
            configuration
        ) {
            documentablesTransformationStage = { module ->
                val registry = module.classlike("Registry")
                registry.descriptionBlocks() shouldContainExactly listOf(
                    "Keeps the codecs.",
                    "Implementation Note: Not thread-safe."
                )
                registry.properties.single { it.name == "size" }
                    .descriptionBlocks() shouldContainExactly listOf(
                        "The number of codecs.",
                        "API Note: Grows as codecs register."
                    )
                module.packages.flatMap { it.typealiases }.single { it.name == "Names" }
                    .descriptionBlocks() shouldContainExactly listOf(
                        "Names of codecs.",
                        "Implementation Note: Kept sorted."
                    )
            }
        }
    }

    @Test
    fun `render a note of a comment without a description`() {
        val writer = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/java/sample/Cache.java
            |package sample;
            |
            |/**
            | * @implNote Entries never expire.
            | */
            |public class Cache {
            |}
            """,
            configuration,
            pluginOverrides = listOf(writer)
        ) {
            documentablesTransformationStage = { module ->
                module.classlike("Cache").descriptionBlocks() shouldContainExactly listOf(
                    "Implementation Note: Entries never expire."
                )
            }
            renderingStage = { _, _ ->
                writer.page("root/sample/-cache/index.html") shouldContain "Entries never expire."
            }
        }
    }

    @Test
    fun `render notes of a constructor and a method on their own pages`() {
        val writer = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/java/sample/Envelope.java
            |package sample;
            |
            |/**
            | * Holds a message.
            | */
            |public class Envelope {
            |
            |    /**
            |     * Creates an empty envelope.
            |     *
            |     * @implNote Allocates no buffer.
            |     */
            |    public Envelope() {
            |    }
            |
            |    /**
            |     * Seals the envelope.
            |     *
            |     * @apiNote Prefer sealing once.
            |     */
            |    public void seal() {
            |    }
            |}
            """,
            configuration,
            pluginOverrides = listOf(writer)
        ) {
            documentablesTransformationStage = { module ->
                val envelope = module.classlike("Envelope") as DClass
                envelope.constructors.single().descriptionBlocks() shouldContainExactly listOf(
                    "Creates an empty envelope.",
                    "Implementation Note: Allocates no buffer."
                )
                envelope.functions.single { it.name == "seal" }
                    .descriptionBlocks() shouldContainExactly listOf(
                        "Seals the envelope.",
                        "API Note: Prefer sealing once."
                    )
            }
            renderingStage = { _, _ ->
                val classPage = writer.page("root/sample/-envelope/index.html")
                classPage shouldContain "Creates an empty envelope."
                classPage shouldContain "Seals the envelope."
                classPage shouldNotContain "Allocates no buffer."
                classPage shouldNotContain "Prefer sealing once."
                writer.page("root/sample/-envelope/-envelope.html") shouldContain
                        "Allocates no buffer."
                writer.page("root/sample/-envelope/seal.html") shouldContain
                        "Prefer sealing once."
            }
        }
    }

    @Test
    fun `render a note in KDoc`() {
        val writer = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/sample/Unpacker.kt
            |package sample
            |
            |/**
            | * Unpacks messages.
            | *
            | * @implNote Unpacking is lazy.
            | */
            |public class Unpacker
            """,
            configuration,
            pluginOverrides = listOf(writer)
        ) {
            documentablesTransformationStage = { module ->
                module.classlike("Unpacker").descriptionBlocks() shouldContainExactly listOf(
                    "Unpacks messages.",
                    "Implementation Note: Unpacking is lazy."
                )
            }
            renderingStage = { _, _ ->
                writer.page("root/sample/-unpacker/index.html") shouldContain
                        "Unpacking is lazy."
                val packagePage = writer.page("root/sample/index.html")
                packagePage shouldContain "Unpacks messages."
                packagePage shouldNotContain "Unpacking is lazy."
            }
        }
    }
}

/**
 * A Java class with a note holding a link and a list, like `AnyPacker` of Spine Base.
 */
private const val PACKER_SOURCE = """
    |/src/main/java/sample/Packer.java
    |package sample;
    |
    |import java.util.List;
    |
    |/**
    | * Packs messages into envelopes.
    | *
    | * <p>Use it for messages of any type.
    | *
    | * @implNote Packing is delegated to {@link List#add(Object)}, which keeps
    | *  the message.
    | *  <ul>
    | *      <li>An envelope refuses to unpack into another type.
    | *  </ul>
    | */
    |public class Packer {
    |}
    """

private fun DModule.classlike(name: String): DClasslike =
    packages.flatMap { it.classlikes }.single { it.name == name }

private fun Documentable.description(): Description =
    documentation.values.single().children.filterIsInstance<Description>().single()

/**
 * Obtains the text of the top-level blocks of the description, with whitespace collapsed.
 */
private fun Documentable.descriptionBlocks(): List<String> =
    description().root.children.map { it.text.replace(Regex("\\s+"), " ").trim() }

/**
 * Computes the brief of this documentable as the Javadoc format does.
 *
 * The HTML format computes the brief of a Java element in the same way.
 */
private fun Documentable.brief(): String =
    firstSentenceBriefFromContentNodes(descriptionContent()).joinToString("") { it.text }.trim()

/**
 * Builds the content of the description as the Javadoc format does.
 */
private fun Documentable.descriptionContent(): List<ContentNode> =
    DocTagToContentConverter().buildContent(
        description().root,
        DCI(setOf(dri), ContentKind.Comment),
        sourceSets
    )

private val DocTag.text: String
    get() = if (this is Text) body else children.joinToString("") { it.text }

private val ContentNode.text: String
    get() = if (this is ContentText) text else children.joinToString("") { it.text }

/**
 * Obtains the rendered page at the given path, failing with the list of pages if missing.
 */
private fun TestOutputWriterPlugin.page(path: String): String {
    writer.contents.keys shouldContain path
    return writer.contents.getValue(path)
}
