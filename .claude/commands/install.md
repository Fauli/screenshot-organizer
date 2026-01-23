# Install APK on Device

Build, install, and launch the debug APK on a connected Android device.

## Steps

1. Run `./gradlew installDebug` to build and install the app
2. If installation fails due to no connected device, show available devices with `adb devices`
3. On success, launch the app with `adb shell am start -n com.screenshotvault/.MainActivity`
4. Report success or failure to the user

## Notes

- Requires Android SDK with ANDROID_HOME set or local.properties configured
- Device must be connected via USB with USB debugging enabled
- For wireless debugging, device must be paired first
