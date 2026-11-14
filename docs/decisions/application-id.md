# Application ID

Decision: The Android application ID and the Kotlin package root are `io.github.jwp23.koj`.

Rationale: The ID is derived from the GitHub account that hosts the public repository, so it
depends on no domain registration. Android and Obtainium identify the installed app by this ID;
changing it after the first install creates a separate app and strands the existing data, so it
is fixed before the first release.
