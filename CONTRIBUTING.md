# Contributing to koj

## Reporting Bugs and Proposing Features

Open a GitHub issue.

## Scope

koj is a single-user Android app. It has no backend, no accounts, and no iOS version, and it
never gives medical or dosing advice. A change that adds any of those needs an accepted ADR
first. The repository is public: no personal data and no credentials go into it.

## Getting Set Up

You need Linux on x86_64, git, and these tools:

| Tool | Why | How to get it |
|------|-----|---------------|
| JDK 25 | Runs the build | Your distribution's package, for example `jdk25-openjdk` on Arch |
| betterleaks | Scans commits for secrets | https://github.com/betterleaks/betterleaks |
| ktfmt, osv-scanner | Format check, dependency audit | `scripts/install-check-tools.sh` |
| Android SDK and emulator | Building, and device tests | [docs/device-verification.md](docs/device-verification.md) |

JDK 25 does not have to be your default Java. The build finds it in the usual system locations
and uses it whatever `java` on your `PATH` is.

`scripts/install-check-tools.sh` downloads pinned versions of ktfmt and osv-scanner, verifies
their checksums, and puts them in `~/.local/bin`, which must be on your `PATH`.

Then enable the git hooks:

```sh
scripts/setup-hooks.sh
```

and check that everything works:

```sh
./gradlew test lint
```

## What the Hooks Do

- **Before each commit:** scan the staged changes for secrets, check formatting, check that the
  app module holds only wiring, audit the locked dependencies for known vulnerabilities, then run
  the tests and lint. A missing tool blocks the
  commit; it is never skipped.
- **On the commit message:** require one Conventional Commits line with no body.
- **Before each push:** if the push changes anything under `app/`, run the device tests. The push
  is blocked when no emulator or device is connected, or when the device tests cover less than
  80 percent of the app module's handwritten code.

Do not skip or disable a hook. If one is wrong, fix the hook.

To fix formatting, run `ktfmt --kotlinlang-style` on the files the hook names.

## Tests Come First

Write a failing test, watch it fail for the reason you expect, then write the code that makes it
pass. A bugfix needs a test that reproduces the bug. Markdown, build configuration, and CI
workflow files are exempt.

```sh
./gradlew test                   # logic tests on the JVM; no device needed
./gradlew connectedAndroidTest   # storage, screens, notifications; needs a running emulator
```

The intake model's tests use expected values worked out by hand, with the working shown in a
comment. Never paste the code's own output into a test as the expected value.

Test data is invented. Never use a real person's data.

## Dependencies

Versions live only in `gradle/libs.versions.toml`. A new dependency needs a decision doc first.

Dependencies are locked. After adding, removing, or upgrading one, regenerate the lockfiles and
commit them with the change:

```sh
./gradlew :app:dependencies :core:dependencies --write-locks
```

## Recording Decisions

A technical choice is written down before it is implemented: an ADR in `docs/adr/` for
architecture and major libraries, a decision doc in `docs/decisions/` for tools, conventions, and
configuration. See [docs/decision-recording.md](docs/decision-recording.md).

## Releasing

The maintainer makes a release by pushing a tag `vMAJOR.MINOR.PATCH` on a commit of `main`, for
example `git tag v0.2.0 && git push origin v0.2.0`. MINOR and PATCH run from 0 to 99.

The Release workflow builds, tests, signs, and publishes `koj-<version>.apk` as a GitHub Release.
Obtainium on the phone installs and updates the app from those releases. Each tag must be higher
than the last, because Android refuses an update with a lower version code.

The signing key is held in the `release` environment's secrets and in an offline copy. It is never
committed. Losing it means the app can no longer be updated in place. The reasons for these
choices are in [docs/decisions/release-versioning-and-signing.md](docs/decisions/release-versioning-and-signing.md).

## Commits and Pull Requests

- Never commit to `main`. Work on a branch named `feat/`, `fix/`, `chore/`, `docs/`,
  `refactor/`, or `test/` plus a short description.
- A commit message is a single Conventional Commits line, for example
  `feat: log a drink with one tap`. No body, no footer.
- A pull request title follows the same rule. Pull requests are squash-merged, so the title
  becomes the commit on `main`. CI rejects a title that does not conform.
- CI runs the same checks as the hooks, plus Semgrep, SonarQube, and a build.

## Project Layout

| Path | Contents |
|------|----------|
| `app/` | The Android app: screens, storage, reminders, backup |
| `core/` | Pure Kotlin with no Android imports: the intake model and the reminder rules |
| `docs/designs/` | How each part is meant to work |
| `docs/adr/`, `docs/decisions/` | Why things are the way they are |
| `scripts/` | Hook and CI check scripts |
| `tests/` | Tests for the scripts |

## Working With an AI Coding Agent

Agent-driven work in this repository requires [beads](https://github.com/gastownhall/beads)
(`bd`). The agent instructions in [AGENTS.md](AGENTS.md) direct agents to track their tasks and
persistent memory in beads, and the committed Claude Code session hook runs `bd prime` at the
start of every session. Without `bd` installed, that hook fails.

Contributing by hand does not require beads.
