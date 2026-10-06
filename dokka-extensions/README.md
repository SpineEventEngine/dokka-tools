# Dokka extensions
## General

The module contains custom Dokka plugins. The list of plugins can be found below:
- `ExcludeInternalPlugin` excludes the code annotated with `io.spine.annotation.Internal`.
- `NoteTagsPlugin` renders the Javadoc block tags `@apiNote`, `@implSpec`, and `@implNote`.

## Usage

Dokka discovers `org.jetbrains.dokka.plugability.DokkaPlugin` subclasses on its classpath during 
setup using `java.util.ServiceLoader`. The way you use this module is provided below:

```Kotlin
dependencies {
    dokkaPlugin("io.spine.tools:dokka-extensions:${version}")
}
```

As the result of the above configuration, all plugins from this module are added to Dokka's 
classpath and automatically applied.

Versions before `2.0.0-SNAPSHOT.9` are published as `io.spine.tools:spine-dokka-extensions`.

## Notes on API and implementation

The documentation of the JDK uses the block tags `@apiNote`, `@implSpec`, and `@implNote`.
Dokka parses them, but renders none of them. `NoteTagsPlugin` moves these notes to the end of
the description of the documented element, so they appear in both the HTML and the Javadoc
formats. This works for Javadoc in Java code and for KDoc in Kotlin code.

Each note starts with its title, as in the documentation of the JDK: "API Note:",
"Implementation Requirements:", or "Implementation Note:". The HTML format shows the title
in bold, and the Javadoc format shows it as plain text. The notes follow this order.
Notes of the same kind keep their order in the comment.

The brief of an element in summary tables does not change, with two exceptions.
The HTML format takes the first sentence of a Java description or the first paragraph of
a KDoc one as the brief, and the Javadoc format takes the first sentence of either. So:
- If a comment has notes but no description, the brief is the beginning of the first note,
  title included.
- If a description has no sentence terminator, such as a period, where the first sentence
  is taken, the brief runs on into the first note.
