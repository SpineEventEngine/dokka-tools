---
slug: render-note-tags
branch: render-note-tags
owner: claude
status: in-review
started: 2026-10-05
---

## Goal

The Dokka documentation of Spine SDK repositories, in both the HTML and the Javadoc
publications, renders the Javadoc block tags `@apiNote`, `@implSpec` and `@implNote`
(dropped by Dokka today) with the JDK titles "API Note", "Implementation Requirements"
and "Implementation Note". The first-sentence brief of documented elements stays as it is.
Delivered as a new plugin in `dokka-extensions`, loaded by consumers the same way as
`ExcludeInternalPlugin`.

## Context

- Repository state: `master` fast-forwarded to `origin/master` (`057439b`,
  `2.0.0-SNAPSHOT.9`); `./config/pull` run (`config` @ `94a9e08b`, shared agents
  submodule `.agents/shared` added). Those changes are in the working tree, uncommitted.
- Dokka is `2.2.0` both here and in consumers (`base-libraries`).
- Dokka internals (read at `v2.2.0`):
  - Java analysis (`JavaPsiDocCommentParser.parseDocTag`) keeps an unknown block tag as
    `CustomTagWrapper(root, name)`; its body is parsed like the description
    (`parseAsParagraph`), links and lists included.
  - HTML (`DefaultPageCreator.contentForDescription`): description, then custom tags via
    `CustomTagContentProvider` (only `SinceKotlinTagContentProvider` is registered), then
    return/since, params, see also, throws. Custom tags are keyed by name, so repeated tags
    collapse into one.
  - Javadoc (`JavadocPageCreator`): renders only `Description` (plus return, since,
    author); brief = `firstSentenceBriefFromContentNodes(description)`. Its HTML translator
    (`JavadocContentToHtmlTranslator`) drops `ContentHeader`, renders every `ContentList`
    as `<ul>`, and bolds only `TextStyle.Bold`, while the `B` DocTag yields
    `TextStyle.Strong`.
  - Brief: Java = first sentence of `Description` (a `.`/`?`/`!` followed by whitespace);
    Kotlin = first paragraph.
- Usage: `base-libraries` + `core-jvm` have 50 `@apiNote`, 48 `@implNote`, 12 `@implSpec`,
  nearly all in Java.

## Design decision: (B) a documentable transformer

Recommended over (A), a `CustomTagContentProvider`, because the Javadoc publication never
consults custom-tag providers: with (A) `javadoc.jar`, which IDEs show to library users,
would still lack the notes. (B) is one code path for both formats and both languages
(KDoc custom tags are `CustomTagWrapper`s too).

A hybrid ((A) for HTML, (B) only in Javadoc runs) is not worth it: it needs detecting the
Javadoc format at runtime (reflection or classpath sniffing; reflection needs explicit
approval per the safety rules) for a cosmetic gain.

Rendering:

- The notes are appended after the existing description content.
- Each note's first paragraph starts with an inline bold label, e.g.
  "**Implementation Note:** Unpacking is delegated to…". A note that starts with a
  non-paragraph block (a list, a code block) gets the label as its own paragraph.
- Order: JDK order (API Note, Implementation Requirements, Implementation Note), source
  order within one tag; repeated tags are all kept.
- No description: one is created from the notes.
- The note wrappers are removed; other tags are untouched; elements without notes keep the
  same instances.
- The label is `<strong>` in HTML and plain text in the Javadoc output (its translator
  renders no `Strong`, same as for any bold text in comments).

Known limitations (to be stated in the KDoc and the README):

- A comment with notes but no description: summary tables show the first sentence of the
  first note, after its label. The JDK's javadoc shows nothing there.
- A Java description without a sentence terminator: the first-sentence brief runs on into
  the notes. Both formats use the same algorithm.

## Plan

- [x] Create branch `render-note-tags` from `master`, carrying the uncommitted
      `config/pull` changes.
- [x] Bump the version to `2.0.0-SNAPSHOT.10` (the `bump-version` skill).
- [x] Code, package `io.spine.tools.dokka.plugin`:
  - `NoteTag`: an enum of the three tags, each with its tag name and title.
  - A pure function `DocumentationNode.withNotesInDescription(): DocumentationNode?`,
    returning `null` when there are no notes.
  - `NoteTagsTransformer : DocumentableReplacerTransformer`, overriding the `process*`
    functions for the module, packages, the five classlike kinds, enum entries, functions
    (constructors and property accessors included), properties, parameters, type
    parameters and type aliases; it rewrites only the nodes holding notes.
  - `NoteTagsPlugin : DokkaPlugin`, registering the transformer at
    `DokkaBase.preMergeDocumentableTransformer`; listed in `META-INF/services`.
- [x] Tests: `NoteTagsPluginSpec` on Dokka's `BaseAbstractTest`, JUnit 5 + Kotest. The
      plugin is found by `ServiceLoader`, as in production.
  - Java class with a description and an `@implNote` holding `{@link}` and `<ul>` (like
    `AnyPacker`): the HTML page shows the label and the text; no `implNote` wrapper remains.
  - The brief stays the first sentence: the brief on the HTML package page, plus
    `firstSentenceBriefFromContentNodes`, the function the Javadoc format uses.
  - A comment with only an `@implNote` gets it rendered.
  - All three tags: titles and JDK order.
  - Notes on a method and a constructor; an `@implNote` in KDoc.
  - Elements without notes are unchanged; the plugin loads as a service.
  - Test dependencies in `dokka-extensions/build.gradle.kts`, all at `Dokka.version`:
    `dokka-test-api` and `dokka-base-test-utils` (`testImplementation`),
    `analysis-kotlin-symbols` (`testRuntimeOnly`; K2 is the Gradle plugin's default).
    Declared in the module because `buildSrc` belongs to `config`; adding them to `Dokka` in
    `config` is a follow-up. `failOnVersionConflict()` may need extra forces.
  - The Javadoc renderer is not unit-tested: `javadoc-plugin` on the test classpath would
    be loaded by `ServiceLoader` for every test and switch them all to the Javadoc format.
    Covered by the end-to-end check instead.
- [x] Docs: the module README (the new plugin; artifact `io.spine.tools:dokka-extensions`)
      and KDoc.
- [x] `./gradlew build dokkaGenerate`; dependency reports refreshed (`docs/dependencies`).
- [x] End to end: `./gradlew publishToMavenLocal`; in `base-libraries` (branch
      `memoize-any-unpack`, clean), temporarily set `Dokka.SpineExtensions.version` to
      `2.0.0-SNAPSHOT.10`; run `./gradlew :base:dokkaGenerate`; check that "refuses to
      unpack" and the label appear in
      `base/build/dokka/html/-base -library/io.spine.protobuf/-any-packer/index.html` and in
      `base/build/dokka/javadoc/io/spine/protobuf/AnyPacker.html`, and that `AnyPacker`'s
      brief is unchanged; then `git restore` the edit.
- [x] Report what was verified by running vs. only read, with the diff and a proposed
      commit split. No commit, push or PR without explicit authorization.
- [x] Optional housekeeping: `.idea/.gitignore` (repo-local, from the initial commit) has
      `!/misc.xml`, which re-includes the `.idea/misc.xml` that `config/pull` untracked.
      Drop that line.

Out of scope: rolling the new version out to consumers (`Dokka.SpineExtensions.version`
in `config`); upstream support in Dokka (Kotlin/dokka#1618).

## Log

- 2026-10-05 — drafted, awaiting approval.
- 2026-10-05 — approved, executing; branch `render-note-tags` created.
- 2026-10-05 — implemented; `./gradlew build dokkaGenerate` green (16 tests).
  `kotlin-test` forced in the module (2.0.21 from `dokka-base-test-utils` vs 2.4.10).
  Two detekt `ReturnCount` findings fixed by restructuring. Mutation check: without
  the service entry, the 5 plugin-level tests fail.
- 2026-10-05 — end to end in `base-libraries` passed: `AnyPacker` shows the note in
  both formats; its brief is unchanged on both package pages. Temporary edit reverted.
- 2026-10-05 — independent review: no correctness bugs found. Fixed the doc
  inaccuracies it found (brief rules per format; the title is bold only in HTML), added
  `@param context`, and added tests for more element kinds, fallback roots, and the
  paragraph-only structure the Javadoc format needs. Top-level constants stay
  `SCREAMING_SNAKE`: detekt's `TopLevelPropertyNaming` requires it, as in 60 other
  Spine files. Build green with 20 tests; the end-to-end check passed again.
