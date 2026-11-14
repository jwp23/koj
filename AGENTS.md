# Agent Instructions

Instructions for AI coding agents working in this repository. Human contributors want
[CONTRIBUTING.md](CONTRIBUTING.md).

These rules belong to the project, not to any one tool. They assume no particular agent harness,
plugin, or slash command.

## What This Project Is

koj is a single-user native Android app that logs caffeine and L-theanine intake, graphs the
estimated amount of each in the body through the day, and prompts for awake, focus, and calm
ratings so the user can compare sources and ratios. Data stays on the phone and is backed up as
a file to cloud storage.

### Out of Scope

Do not add these without an accepted ADR first:

- A backend, user accounts, or multi-device sync
- iOS or any cross-platform layer
- Medical or dosing advice. The app shows estimates and the user's own ratings; it never
  recommends what to take. This applies to every user-facing string.
- Personal data or credentials in the repository. The repository is public. Sources, doses, and
  settings are app data; keys are CI secrets.

## Tech Stack

Kotlin; Jetpack Compose; Room (SQLite); AlarmManager and WorkManager; kotlinx.serialization;
JUnit 4, AndroidX Test, Compose UI Test; Gradle; GitHub Actions.

Versions live only in `gradle/libs.versions.toml`. The minimum Android version is set in the
build file; do not add version checks or compatibility code beneath it.

## Code Style

- The intake model and the reminder rules contain no Android imports. They live in a plain
  Kotlin module so they test on the JVM. Android code calls them; they never call Android.
- Logic never reads the clock directly. Pass the current time in.
- Store times as instants with their time zone, never as local date-time strings.
- Every default pharmacokinetic value cites its source in a comment where it is defined. Do not
  introduce a number from memory.
- Test fixtures use invented values, never a real person's data.
- A new dependency needs a decision doc first. Each one is supply-chain exposure.
- Name things for what they do in the domain. Comments explain what and why, never what changed.

When a convention and simplicity conflict, simplicity wins.

## Testing

```sh
./gradlew test                   # logic tests on the JVM; no device needed
./gradlew connectedAndroidTest   # storage, screens, notifications; needs a running emulator
```

Use red/green TDD for every feature and every bugfix: write a failing test, watch it fail for
the reason you expect, write the minimum code to pass, refactor while green. A bugfix without a
test that reproduces the bug is incomplete. Markdown, build configuration, and CI workflow files
are exempt.

| Code under test | Where it is tested |
|-----------------|--------------------|
| Intake model, reminder rules | JVM. Must pass with no emulator running. |
| Storage, backup and restore | Emulator, against a real database. |
| Screens, notification taps | Emulator. |
| Lock-screen behaviour | Manual checklist on a device; record the result in the pull request. |

- Name each test for the behaviour it verifies.
- NEVER assert on a mock's behaviour in place of real logic. NEVER mock in an emulator test.
- Test output MUST be clean. When a test deliberately triggers an error, capture that output and
  assert on it.
- Intake model tests use hand-calculated expected values, with the working shown in a comment.
  Never copy the code's own output into a test as the expected value.

## Debugging

Find the root cause. A change that suppresses a symptom without explaining the cause is not a
fix.

## Recording Decisions

Record a technical choice before implementing it:

- **ADR** — `docs/adr/NNN-short-description.md` — language, framework, or major library
  selection; architecture patterns; testing strategy; CI/CD design.
- **Decision doc** — `docs/decisions/short-description.md` — tool selection, conventions, file
  organization, configuration, hook setup, a new dependency.

Formats and numbering: `docs/decision-recording.md`.

## Git Workflow

- NEVER commit directly to `main`. Every change goes through a feature branch and a pull request.
- Branch names: `feat/`, `fix/`, `chore/`, `docs/`, `refactor/`, `test/` plus a short
  description.
- Commit messages are a single Conventional Commits line — no body, no footer.
- Pull request titles follow the same convention. Merges are squashed, so the title becomes the
  commit subject on `main`. CI rejects a title that does not conform.
- Commit the version catalog and the Gradle wrapper.
- Larger changes use git worktrees in `.worktrees/`.
- Make the smallest reasonable change that achieves the outcome.
- NEVER discard or rewrite an existing implementation without asking first.

## Local Checks

`scripts/setup-hooks.sh` enables the committed hooks.

- `pre-commit` scans staged changes for secrets, then runs format, lint, the dependency audit,
  and `./gradlew test`. A missing secrets scanner blocks the commit.
- `commit-msg` checks the commit convention.
- `pre-push` runs `./gradlew connectedAndroidTest` when the push touches storage, screens, or
  notifications, and blocks if no emulator or device is connected.

NEVER skip, evade, or disable a hook.

## Issue Tracking

Report bugs and propose features as GitHub issues. Work that changes behaviour should trace back
to one.

## Non-Interactive Shell Commands

`cp`, `mv`, and `rm` are aliased to interactive mode on some systems, which hangs an agent
waiting for input. Always pass the non-interactive flag: `cp -f`, `mv -f`, `rm -f`, `rm -rf`,
`cp -rf`.

## Reference Documents

**IMPORTANT:** Before starting any task, identify which docs below are relevant and read them
first. Load the full context before making changes.

- `docs/designs/app-overview.md` — changing how the parts fit together, the stored data, or a
  screen. The map of the app.
- `docs/designs/intake-model.md` — touching curve or ratio calculations.
- `docs/designs/reminders.md` — touching any prompt, rule, or notification.
- `docs/designs/backup.md` — touching backup or restore.
- `docs/adr/*.md` — a change touches an architectural decision, or you need to know why one was
  made.
- `docs/decisions/*.md` — working in an area a recorded choice covers.
- `docs/decision-recording.md` — recording a decision.
- `docs/tech-stack-docs.md` — working against a library's API.
- `docs/device-verification.md` — verifying anything on an emulator or phone.
