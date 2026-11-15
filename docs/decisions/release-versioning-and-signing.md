# Release Versioning and Signing

Decision: A release is made by pushing a tag `vMAJOR.MINOR.PATCH`. The release workflow derives
the version from the tag: the version name is the tag without the `v`, and the version code is
`MAJOR * 10000 + MINOR * 100 + PATCH`, so minor and patch stop at 99. The workflow passes both to
Gradle as the properties `koj.versionName` and `koj.versionCode`. Each release carries exactly
one APK. The build does not do this yet: `app/build.gradle.kts` still sets the version as literals, and
the release workflow is not written.

The build signs the release APK when the environment variables `KOJ_KEYSTORE_FILE`,
`KOJ_KEYSTORE_PASSWORD`, `KOJ_KEY_ALIAS`, and `KOJ_KEY_PASSWORD` are set, and leaves it unsigned
otherwise. In CI they come from secrets of a GitHub environment named `release` that only `v*`
tags may deploy to. The maintainer generates the key and keeps the only other copy offline. The
build does not read these variables yet.

Rationale: Obtainium takes a release's version from its tag and compares it with the installed
app's version name, so the two must agree; deriving both from the tag makes disagreement
impossible and needs no version-bump pull request. Android refuses an update with a lower
version code or a different signing certificate, so the code must rise with every release and
the key must never change. The code is computed from the tag rather than taken from a run
counter so that the same tag always builds the same version.

Environment secrets are released only to jobs that use the environment, and the tag restriction
keeps them from a workflow run on a branch. GitHub does not pass secrets to workflows triggered
from forks.

No build provenance attestation is published; one user installs the app, from releases that the
same person tags.

Sources, checked 2026-11-14: Obtainium's wiki (https://wiki.obtainium.imranr.dev/app_tracking/),
https://developer.android.com/studio/publish/versioning,
https://developer.android.com/studio/publish/app-signing, and
https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-deployments/managing-environments-for-deployment.
