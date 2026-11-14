---
name: verify-on-device
description: Use when a change touches anything under app/, before pushing it, or when asked to run the device tests, start or stop the emulator, or verify behaviour on a device. Covers when device verification is required, what a pass looks like, what to record, and cleanup.
---

# Verify on Device

`docs/device-verification.md` is the single source for every command. Open it and use the
commands from the section named below. Do not paraphrase them from memory.

## When

- Any change under `app/` needs the device tests to pass before you push. The `pre-push` hook
  runs them when a push changes anything under `app/`, and blocks the push when no emulator or
  phone is connected. Do not bypass the hook; connect a device.
- Changes that touch nothing under `app/` do not need a device.
- Lock-screen behaviour cannot be checked on the emulator. Check it by hand on a phone and
  record the result in the pull request.

## Steps

1. Check whether a device is already connected (the "Start the Emulator" section shows how
   connected devices are listed). If one is listed as `device`, skip to step 3.
2. If the emulator or SDK is missing, follow "One-Time Setup" in the document. Otherwise follow
   "Start the Emulator" and wait for it to finish booting. The document sets the time limit. If the
   limit passes, stop it as "Stop the Emulator" describes for one that has not finished booting,
   and tell your human partner; a device check that did not run is not a pass, however urgent the push.
3. Run the tests from "Run the Device Tests".
4. Stop the emulator as the "Stop the Emulator" section describes, even if the tests failed, but only
   after the last step that needs a device. The `pre-push` hook runs the tests again, so stopping
   before the push makes the hook fail with no device. Only stop an emulator you started; leave a
   phone alone.

## What a pass looks like

The Gradle task finishes successfully and the report named in the document shows no failed
tests. `No connected devices!` is a failure to verify, not a pass: start a device and rerun.

## What to record

In the pull request, state that the device tests passed, or why they were not run. Add the
result of the manual lock-screen check when the change affects it.

## Cleanup

Leave no emulator process running when you finish. Check the device listing described in
"Start the Emulator" and confirm no emulator you started remains in it. Also run the process
check in "Stop the Emulator".
