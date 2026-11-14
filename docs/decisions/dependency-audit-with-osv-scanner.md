# Dependency Audit With osv-scanner

Decision: Dependencies are audited for known vulnerabilities by osv-scanner reading committed
Gradle lockfiles. Dependency locking is strict and covers every compile and runtime classpath of
both modules, including test classpaths. It does not cover the build tooling's own classpaths
(the lint tool, the device test platform, Gradle plugins). The tracked `settings-gradle.lockfile`
holds no dependency coordinates, only an `empty=` marker for the version catalog, so it is neither
audited nor rewritten by the regenerate command. After any dependency change the
lockfiles are regenerated with
`./gradlew :app:dependencies :core:dependencies --write-locks`. In CI, GitHub's dependency review
also checks each pull request.

Rationale: osv-scanner is a single binary that answers in under a second, so it can run on every
commit, and it adds no Gradle plugin. It reads lockfiles rather than build scripts, which is why
locking is enabled; the lockfiles also make builds repeatable and give Dependabot a file to
update. The OWASP dependency-check plugin was rejected because it needs the full NVD database and
is too slow for a hook.

Locking the tooling classpaths as well was tried and rejected. The Android Gradle Plugin's lint
and test tooling pull in libraries with more than a hundred published advisories. None of that
code is compiled into or shipped with the app, the project cannot change those versions, and the
audit would fail from the first commit. Those versions are pinned by the plugin versions instead.

Gradle's dependency verification (`verification-metadata.xml`) is not used. Dependabot cannot
update that file, so every dependency pull request would fail until someone regenerated it by
hand.

Checked 2026-11-14.
