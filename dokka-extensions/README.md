# Dokka extensions
## General

The module contains custom Dokka plugins. The list of plugins can be found below:
- `ExcludeInternalPlugin`

## Usage

Dokka discovers `org.jetbrains.dokka.plugability.DokkaPlugin` subclasses on its classpath during 
setup using `java.util.ServiceLoader`. The way you use this module is provided below:

```Kotlin
dependencies {
    dokkaPlugin("io.spine.tools:spine-dokka-extensions:${version}")
}
```

As the result of the above configuration, all plugins from this module are added to the Dokka's 
classpath and automatically applied.
