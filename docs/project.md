# Project: dokka-tools

## Overview

`dokka-tools` holds the Spine SDK extensions to [Dokka][dokka], the documentation
engine for Kotlin and Java code. It publishes one artifact,
`io.spine.tools:dokka-extensions`: a set of Dokka plugins that tailor the API
documentation of every Spine SDK repository. The shared build configuration from
[`config`][config] adds the artifact to the Dokka classpath of each project
(`useDokkaWithSpineExtensions()` in `DokkaExts.kt`), pinned by
`Dokka.SpineExtensions.version`.

## Architecture

Role: **tool**, a library of Dokka plugins. The Gradle build has one published
module, `dokka-extensions`. Dokka finds the plugins with `java.util.ServiceLoader`
through `META-INF/services/org.jetbrains.dokka.plugability.DokkaPlugin`, so adding
the artifact applies all of them; there is no per-plugin configuration.

The plugins, in the `io.spine.tools.dokka.plugin` package:

- `ExcludeInternalPlugin` removes the declarations annotated with
  `io.spine.annotation.Internal` from the documentation.
- `NoteTagsPlugin` renders the Javadoc block tags `@apiNote`, `@implSpec`, and
  `@implNote`, which Dokka parses but drops. It moves them to the end of
  the descriptions of the documented elements, so they appear in both
  the HTML and the Javadoc formats.

Both plugins are documentable transformers which work before the documentables
of different source sets are merged.

Key constraints:

- The plugins run inside the Dokka of every consumer. They compile against
  `Dokka.version` declared in `config`, which must match the Dokka the consumers
  run. The plugin API of Dokka is a preview (`@DokkaPluginApiPreview`) and may
  change between Dokka versions, so re-check the extension points on each bump.
- Spine applies both the HTML and the Javadoc formats of Dokka. The Javadoc format
  renders only a part of Dokka's content model: it ignores custom tags and drops
  headers. Check a change of the rendered content in both formats.
- Keep the runtime dependencies minimal: they land on the Dokka classpath of every
  consumer. This is why the module depends on `spine-annotations`, not `spine-base`.
- The tests run Dokka on inline sources with Dokka's test API (`BaseAbstractTest`).
  Dokka finds the plugins of this module on the test classpath with
  `ServiceLoader`, as it does for the consumers. Keep `javadoc-plugin` off that
  classpath: it would switch every test to the Javadoc format.
- To check a change against real code, publish it to Maven Local with a bumped
  version, set `Dokka.SpineExtensions.version` of a consumer to that version
  locally, and run `./gradlew dokkaGenerate` there.
- The artifact is named `dokka-extensions` since `2.0.0-SNAPSHOT.9`; earlier
  versions are published as `spine-dokka-extensions`. Consumers get a new version
  when `config` updates `Dokka.SpineExtensions.version`.

Read [`.agents/guidelines/jvm-project.md`](../.agents/guidelines/jvm-project.md) for
build stack, coding style, tests, and versioning.

[dokka]: https://github.com/Kotlin/dokka
[config]: https://github.com/SpineEventEngine/config
