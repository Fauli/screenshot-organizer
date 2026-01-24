# Install APK on Device

Build, install, and launch the debug APK on a connected Android device.

## Configuration

- ADB path: `~/Library/Android/sdk/platform-tools/adb`

## Steps

1. Run `./gradlew installDebug` to build and install the app
2. If installation fails due to no connected device, show available devices with `~/Library/Android/sdk/platform-tools/adb devices`
3. On success, launch the app with `~/Library/Android/sdk/platform-tools/adb shell am start -n com.screenshotvault/.MainActivity`
4. Report success or failure to the user

## Notes

- Device must be connected via USB with USB debugging enabled
- For wireless debugging, device must be paired first
