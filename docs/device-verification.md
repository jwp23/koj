# Device Verification

How to run koj's device tests. Storage, screens, and notifications are tested on an Android
emulator; `./gradlew connectedAndroidTest` runs those tests on whatever emulator or phone is
connected.

Everything here installs under your home directory. Nothing needs root.

## One-Time Setup

Hardware acceleration is required. Check that `/dev/kvm` exists and that your user can read and
write it.

Install the Android SDK, the emulator, and an Android 17 system image. The system image is a
2 GB download and 5 GB on disk.

```sh
export ANDROID_HOME="$HOME/Android/Sdk"
download=$(mktemp -d)
curl --fail --silent --show-error --location \
    --output "$download/cmdline-tools.zip" \
    https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
echo "e025545c62a8e64c7559119566a569fb1dec5f60  $download/cmdline-tools.zip" | sha1sum --check -
mkdir -p "$ANDROID_HOME/cmdline-tools"
unzip -q "$download/cmdline-tools.zip" -d "$ANDROID_HOME/cmdline-tools"
mv -f "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
rm -rf "$download"

sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$sdkmanager" --sdk_root="$ANDROID_HOME" --licenses
"$sdkmanager" --sdk_root="$ANDROID_HOME" \
    "platform-tools" "platforms;android-37.0" "emulator" \
    "system-images;android-37.0;google_apis;x86_64"
```

`sdkmanager` prints a notice that it is deprecated in favour of a newer `android` command. The
commands above still work and are the ones this project has verified.

Create the emulator the tests run on:

```sh
"$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n koj-api37 \
    -k "system-images;android-37.0;google_apis;x86_64" -d pixel_10a
```

Tell the build where the SDK is. Run this in the repository root; `local.properties` is ignored
by git.

```sh
printf 'sdk.dir=%s\n' "$HOME/Android/Sdk" > local.properties
```

The first build downloads the SDK build tools it needs.

## Start the Emulator

```sh
sdk="$HOME/Android/Sdk"
"$sdk/emulator/emulator" -avd koj-api37 -no-window -no-audio -no-boot-anim -no-snapshot \
    -gpu swiftshader_indirect > /dev/null 2>&1 &
booted=
for _ in $(seq 120); do
    if [ "$("$sdk/platform-tools/adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then
        booted=1
        break
    fi
    sleep 1
done
[ -n "$booted" ] || echo "The emulator did not finish booting in 120 seconds. Stop it (see Stop the Emulator)." >&2
```

The loop ends when Android has finished starting, which for a headless boot took 14 seconds when
measured on the development machine, and usually takes about twenty. It gives up after 120 seconds, several times the normal boot, and prints
what to do. Drop `-no-window` to watch the screen.

`adb devices` lists the emulator as `emulator-5554	device` once it is usable. While it is still
starting it is listed as `offline`.

## Run the Device Tests

```sh
./gradlew connectedAndroidTest
```

The report is `app/build/reports/androidTests/connected/debug/index.html`.

With no emulator or device connected the task fails with `No connected devices!`.

To check how much of the app module the device tests cover, run this instead:

```sh
./gradlew createDebugAndroidTestCoverageReport
```

Its HTML report is `app/build/reports/coverage/androidTest/debug/connected/index.html`.

The `pre-push` hook runs this coverage form when a push changes anything under `app/`, and blocks
the push when nothing is connected or when coverage is below 80 percent. It then runs
`scripts/check-app-complexity.sh` on the same report and blocks the push when a view-model or store
method is more complex than its maximum.

## Stop the Emulator

```sh
"$HOME/Android/Sdk/platform-tools/adb" -s emulator-5554 emu kill
```

`emu kill` needs `adb` to see the emulator. For one that is still starting, or hung, and that
`adb` cannot reach, stop the process directly:

```sh
pkill -f '[q]emu-system.*-avd koj-api37'
```

The first letter is bracketed so the pattern does not match the shell that runs the command, whose
own command line contains the pattern.

Check that nothing is left. This prints nothing and exits 1 when the emulator is gone:

```sh
pgrep -a qemu
```
