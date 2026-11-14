# App Overview

koj is a single-user Android app for tracking caffeine and L-theanine intake, estimating how much
of each is in the body through the day, and collecting awake, focus, and calm ratings to compare
against what was taken. ADR-001 records the platform choice.

## Purpose

The app answers two questions, in this order of priority:

1. Which sources, combinations, and L-theanine-to-caffeine ratios give the best daytime focus and
   calm, and get the user awake soonest?
2. How much caffeine remains at bedtime, and how does that relate to sleep?

## Parts

Each part has one job. Dependencies point one way.

| Part | Job | Depends on |
|------|-----|------------|
| Intake model | Computes the amount of each substance in the body at any time from the logged drinks. Pure Kotlin, no Android dependency. | Nothing |
| Storage | Saves and loads sources, drinks, check-ins, and settings in an on-device SQLite database. | Intake model types |
| Reminders | Decides which prompt is due, posts notifications, and handles their taps. | Storage, intake model |
| Screens | Logging, the graph, history, the source list, settings. | Storage, intake model |
| Backup | Writes all data to a file in a user-chosen folder and restores from one. | Storage |

There is no backend and no account. Nothing personal lives in the repository: sources, settings,
and credentials are app data.

## Stored Data

- **Source** — name, caffeine per serving, L-theanine per serving, intake style (sipped or
  fast), default sipping duration, and an optional usual time of day. Either amount may be zero.
  A source with both at zero records a supplement that is tracked as an event only.
- **Drink** — the source, the fraction of a serving, the start time, and the finish time, which
  is empty while a sipped drink is in progress. A drink keeps its own copy of the per-serving
  amounts, so editing a source never changes history.
- **Check-in** — the time, ratings from 1 to 5 for awake, focus, and calm (each optional), and the
  kind of prompt that produced it. In-body amounts at a check-in are computed when needed, not
  stored, so they always reflect the current half-life settings.
- **Settings** — half-lives and absorption times, the check-in schedule and daily cap, morning
  time, evening quiet time, bedtime, and the backup folder.

Times are stored as instants with their time zone, so travel and daylight saving do not shift
history. Data is never pruned.

The graph is never stored. It is recomputed from the drinks, so a changed half-life redraws
every past day.

## Screens

| Screen | Content |
|--------|---------|
| Today | Current in-body caffeine and L-theanine. The day's graph. One-tap buttons for sources. Drinks in progress, each with a Finish button. Warnings for denied notifications and stale backups. |
| Log a drink | Opened when one tap is not enough: a different amount, "just a sip", or an earlier start time. |
| History | Past days with drinks, check-ins, and the graph. Any entry can be corrected or deleted. |
| Sources | The editable source list. |
| Settings | Everything under Settings above, plus the time of the last successful backup. |

### The graph

- Two lines: caffeine and L-theanine in the body over the day.
- Intake markers above the lines: a point for a fast drink or supplement, a bar from start to
  finish for a sipped drink. Tapping one shows the source and amounts.
- Check-in markers on the time axis. Tapping one shows its ratings.
- A projection from now to bedtime, labelled with the caffeine remaining then.

The taken ratio (L-theanine to caffeine, as dosed) appears on each drink and each day. The
in-body ratio is not shown as a number; the two lines show it.

## Failure Handling

- **Notifications denied** — the Today screen shows a banner that explains the app needs them.
- **Device restart** — the app re-creates its scheduled reminders at startup.
- **Edited or deleted drink** — reminders tied to it are recalculated.
- **Backup failure** — see `backup.md`.

## Distribution

The repository is public. CI builds a signed release APK and publishes it as a GitHub Release.
The phone installs and updates the app from those releases with Obtainium. The signing key
lives only in CI secrets and an offline copy, never in the repository.

## Testing

| Code under test | Test kind |
|-----------------|-----------|
| Intake model | Unit tests with hand-checked values |
| Reminder rules | Unit tests of a pure function, one per rule |
| Storage and backup | Tests against a real database, including a round trip through backup and restore |
| Screens and notification taps | Automated tests on an emulator |
| Lock-screen behaviour | Manual checklist on the device |

## Planned, Not Yet Designed

- **Comparison views** — time to awake by first source; focus and calm by ratio and by source
  combination; afternoon tiredness by morning intake and by supplements; month and year trends.
  To be designed once several weeks of data exist.
- **Oura import** — wake time, possibly through Health Connect, and sleep score through the Oura
  API, which requires an OAuth application. To be designed after testing whether sign-in can
  complete on the phone without a server.
- **Dropbox backup** — an additional backup destination through the Dropbox API.
