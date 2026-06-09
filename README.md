# Cordova Marketplace Plugin

Reports how the app was installed / which app marketplace it came from. Exposes
`window.plugins.marketplace` with a single action, `getInfo`.

## Installation

```bash
cordova plugin add @genvid/cordova-plugin-marketplace
```

The plugin is published publicly to npm under the
[`@genvid`](https://www.npmjs.com/package/@genvid/cordova-plugin-marketplace)
scope.

## Development

### File structures

* `plugin.xml`: The plugin configuration file
* `src/`
  * `android/`: Android source code
  * `ios/`: iOS source code
* `www/`: Web source code
* `tests/`: Node Project Plugin Tests
* `types/`: Some type definitions (manually update)
* `demo/`: Node Demo project also use for testing

### How to build the plugin

```bash
npm i
npm run lint
npm run package
```

This will create two packages:

* `genvid-cordova-plugin-marketplace-<version>.tgz` for the plugin
* `cordova-plugin-marketplace-tests-<version>.tgz` for the tests

#### Notice

Please, maintain the `.npmignore` consistently to avoid
distributing unnecessary files in the packages.

### Releasing

Releases are published to npm automatically by GitHub Actions using OIDC
trusted publishing — no npm token is stored anywhere. Pushing a `vX.Y.Z` tag
that matches `package.json`'s `version` triggers
[`.github/workflows/publish.yml`](.github/workflows/publish.yml), which runs the
shared validation gate and then `npm publish --provenance`. `ci.yml` runs the
same gate (lint / typecheck / test / build) on every pull request and push to
`main`.

> The CircleCI `android`/`ios` jobs only build the on-device demo as a smoke
> test; they no longer package or publish the plugin.

### How to build and run the test demo

After having build the packages:

```bash
npm run install-sdk:android:windows
npm run setup:demo
npm run build:demo:<platform>
```

where `<platform>` is replaced with either `android` or `ios`.

Then you can run the application:

```bash
npm run run:demo:<platform>
```

To clean everything and rebuild, you can do:

```bash
npm run refresh:demo:<platform>
```

### Manual installation and running for Android

Sometimes, cordova doesn't select the right ABI to upload.  You will get an error like:

```
Using apk: C:\repos\cordova-plugin-eos\demo\platforms\android\app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk
Package name: com.genvidtech.eosdemo
Command failed with exit code 1: adb -s 38110DLJH000N8 install -r C:\repos\cordova-plugin-eos\demo\platforms\android\app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk
adb: failed to install C:\repos\cordova-plugin-eos\demo\platforms\android\app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk: Failure [INSTALL_FAILED_NO_MATCHING_ABIS: INSTALL_FAILED_NO_MATCHING_ABIS: Failed to extract native libraries, res=-113]
```

Here what to do:

```powershell
# install the right APK
abd install -r .\demo\platforms\android\build\outputs\apk\debug\app-{abi of your android device}-debug.apk
# start the application on the device
adb shell am start -n com.genvidtech.eosdemo/com.genvidtech.eosdemo.MainActivity
# output logs
adb logcat --pid=$(adb shell pidof -s com.genvidtech.eosdemo) | tee eosdemo.log
```
