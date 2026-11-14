# Format Kotlin With ktfmt

Decision: Kotlin sources and Gradle build scripts are formatted with ktfmt in `--kotlinlang-style`,
run as the standalone native binary from the ktfmt release page. It is not applied as a Gradle
plugin.

Rationale: ktlint has no stable release that supports Kotlin 2.4; its maintainer states that 1.8.0
and earlier are incompatible and that 1.x will not be updated. ktfmt is maintained under the
Kotlin GitHub organization, is built against Kotlin 2.4, and has no configuration to argue about.
The native binary checks a file in milliseconds, which suits a hook that runs on every commit, and
it adds no Gradle plugin to the build's supply chain.

Checked 2026-11-14. Revisit ktlint if a stable release supports the project's Kotlin version and
its lint rules are wanted.
