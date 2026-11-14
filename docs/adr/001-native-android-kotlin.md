# ADR-001: Native Android App in Kotlin

## Context

The app tracks caffeine and L-theanine intake on a single Android phone, estimates how much of
each is in the body over the day, and prompts for awake, focus, and calm ratings. It has one
user. It is installed directly on the device, not distributed through a store.

Most of the app's behaviour is reminders: rating buttons on the notification itself, check-ins
timed against a logged drink, nudges that repeat until answered, and reminders that are posted
again after being dismissed. Android 14 and later let the user dismiss ongoing notifications, so
persistence has to be built by re-posting.

The maintainer has no strong language preference and will direct an AI agent that writes most of
the code. Fast iteration and easy refactoring are the stated priorities. The project may be
open-sourced later.

## Decision

Build a native Android app in Kotlin, with Jetpack Compose for screens, an on-device SQLite
database for storage, and Android's own alarm and notification APIs for reminders.

Intake modelling (dose spreading for sipped drinks, elimination curves, ratios) lives in a plain
Kotlin module with no Android dependency, so it is unit-tested on the JVM and can be reshaped
without touching the UI.

There is no backend. Data stays on the phone and is protected by a backup file written to cloud
storage.

Native was chosen because reminders are the hard part of this app and they depend on
Android-specific APIs. Calling those APIs directly removes a plugin layer from the feature the
app depends on most.

## Trade-offs

- **Flutter (Dart).** Faster UI iteration and a path to iOS. Rejected because notifications
  would go through third-party plugins, and iOS is not wanted.
- **Web app wrapped as an Android app (TypeScript).** Most familiar tooling. Rejected because
  reliably timed, interactive local notifications need plugins or a push server, and a push
  server is a backend.
- **Backend with sync.** Rejected for a single-user, single-device app: it adds hosting,
  authentication, and its own backups without serving a need.

Costs accepted: Android SDK and emulator setup on the development machine, builds slower than a
small compiled CLI project, and no iOS version. Notification behaviour on a real lock screen can
only be judged by installing a build on the phone.
