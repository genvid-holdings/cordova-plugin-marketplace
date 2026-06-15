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

### Continuous integration

Native builds run on GitHub Actions (CircleCI has been removed):

* [`android.yml`](.github/workflows/android.yml) and
  [`ios.yml`](.github/workflows/ios.yml) each run a **smoke** build on every pull
  request and push to `main` — Android compiles/links on `ubuntu-latest`, iOS
  builds for the simulator on `macos-15`. These require no secrets.
* On a `vX.Y.Z` tag or a manual **Run workflow** (`workflow_dispatch`), a
  **distribute** job builds a signed artifact for sideloading. Android produces a
  signed `.apk`. The iOS distribute job is wired up but currently **disabled**
  pending a signing identity (a `.p12` that includes the certificate's private
  key) — see the note at the top of that job in `ios.yml`.

Signing material is pulled from 1Password at run time via a single repository
secret, `OP_SERVICE_ACCOUNT_TOKEN` (a service account scoped to the signing
vault); no certificates or keystores are stored in GitHub.

### How to build and run the test demo

After having built the packages:

```bash
npm run setup:demo
npm run setup:demo:<platform>      # adds the cordova platform (android | ios)
npm run build:demo:<platform>      # device build (iOS uses --device)
```

where `<platform>` is replaced with either `android` or `ios`. The Android SDK
(and, for iOS, Xcode) must already be installed on your machine.

Then you can run the application on a connected device/emulator:

```bash
npm run run:<platform>
```

To build for the iOS **simulator** (no signing required — this is what CI's smoke
job runs), use the `:sim` scripts instead:

```bash
npm run setup:demo:sim
npm run setup:demo:sim:ios
npm run build:demo:ios:sim
```

To clean everything and rebuild, you can do:

```bash
npm run refresh:<platform>
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
