# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this plugin does

`@genvid/cordova-plugin-marketplace` is a small Cordova plugin that reports **how the app was installed / which app marketplace it came from**. It exposes a single JS API, `window.plugins.marketplace`, with one native action: `getInfo`.

- **Android**: install source via `PackageManager` — `getInstallSourceInfo()` on API ≥ 30 (S), falling back to `getInstallerPackageName()` on older devices. Returns the initiating/installing/originating/update-owner package names.
- **iOS** (17.4+): `MarketplaceKit`'s `AppDistributor.current` — resolves to `AppStore`, `TestFlight`, `web`, `Other`, or an alternative marketplace's identifier.

The cross-platform contract is the `name` field. Each platform also returns platform-specific extra fields in the same object.

## Architecture

Three layers that must stay in sync — adding or changing an action means touching all of the relevant ones:

1. **`www/index.js`** — the JS bridge. Clobbers `window.plugins.marketplace` (see `plugin.xml` `<clobbers>`). On `onCordovaReady` it calls `getInfo`, caches the result on `.available` / `.name` / `.info`, then fires a **sticky** Cordova channel `onMarketplaceReady`. The plugin registers `channel.waitForInitialization('onMarketplaceReady')`, so `deviceready` is delayed until `getInfo` resolves. Consumers can rely on `.available` / `.name` being populated by `deviceready`.
2. **`src/android/Marketplace.java`** — `CordovaPlugin.execute()` switches on the action string (`getInfo`) and replies via `CallbackContext`. Native package: `com.genvidtech.cordova.marketplace`.
3. **`src/ios/Marketplace.swift`** — `CDVPlugin` subclass; `getInfo` runs an async `Task` and replies via `commandDelegate.send`. Requires the `MarketplaceKit.framework` (declared in `plugin.xml`) and iOS ≥ 17.4.

`types/index.d.ts` is the public TypeScript surface and is **maintained by hand** — update it when the JS API changes.

## Tests & demo

- **`tests/`** is a separate Cordova test package (`cordova-plugin-marketplace-tests`) using the `cordova-plugin-test-framework` convention: `defineAutoTests` (Jasmine specs in `autoTests.js`) and `defineManualTests` (button-driven UI in `manualTests.js` / `testApi.js`). It is **not published to npm** — `npm run package` packs it into a local `.tgz` that the demo (and the CircleCI device-build jobs) install via `file://`.
- **`demo/`** is a Cordova app used as the test harness. It is **generated** by the `setup:demo` scripts (its `platforms/`, `node_modules`, etc. are not real sources) — `npm run clean:demo` wipes it with `git clean -fxd`. Don't hand-edit generated files under `demo/platforms/`.

## Commands

Build & lint the plugin:

```bash
npm i
npm run lint          # eslint .   (lint:fix to autofix)
npm run package       # npm pack → produces plugin .tgz + tests .tgz
```

Run the demo (after `npm run package`):

```bash
npm run setup:demo                 # creates demo cordova project + build.json
npm run setup:demo:android         # or setup:demo:ios
npm run build:demo:android         # or build:demo:ios
npm run run:android                # or run:ios
```

Full clean rebuild in one shot: `npm run refresh:android` / `refresh:ios` (package → clean demo → setup → build), or `restart:android` / `restart:ios` to also run.

The gate scripts `npm run typecheck` (`tsc --noEmit` over `types/index.d.ts`), `npm run test`, and `npm run build` exist only to satisfy the shared CI gate; `test` and `build` are intentional no-ops (the Jasmine specs are device-only and the plugin ships its sources as-is).

### CI / Publishing

- **GitHub Actions** owns packaging and publishing. `.github/workflows/ci.yml` runs the shared `genvid-public-ci` gate (lint / typecheck / test / build) on PRs and pushes to `main`. `.github/workflows/publish.yml` fires on a `vX.Y.Z` tag and publishes `@genvid/cordova-plugin-marketplace` to npm via **OIDC trusted publishing** (`--provenance`, no stored token). The tag must match `package.json`'s `version`. See the **Releasing** section of the README; use the `release-npm-package` skill to cut a release.
- **CircleCI** (`.circleci/config.yml`) now only runs the `android` / `ios` on-device demo build smoke tests (tag filter `/v\d+\..*/`), each packing the plugin locally. It no longer packages, publishes, or uploads to Azure.

### Platform notes

- **iOS** signing: `setup:demo:config` substitutes the `APPLE_DEVELOPMENT_TEAM` env var into `ios-build.json` → `demo/build.json`. That env var must be set, and `xcode` / `ios-deploy` are installed by `setup:demo:ios`.
- **Android ABI mismatch**: Cordova sometimes picks the wrong APK ABI on `run`, failing with `INSTALL_FAILED_NO_MATCHING_ABIS`. Manually `adb install -r` the APK matching the device ABI from `demo/platforms/android/.../apk/debug/`, then launch with `adb shell am start -n com.genvidtech.eosdemo/...MainActivity`. See README for the exact incantation.

## Conventions / gotchas

- Keep **`.npmignore`** accurate — it controls what ships in the published `.tgz`. The README calls this out explicitly.
- Source files carry the Apache-2.0 license header (the plugin itself is **MIT-0**); preserve headers when editing.
- This plugin was forked from an internal **EOS (Epic Online Services)** plugin. Leftover `eos` references remain in the README, the demo app id (`com.genvidtech.eosdemo`), and a test heading ("EOS Tests"). These are stale naming, not separate functionality.
- Repository is hosted on **GitHub** (`genvid-holdings/cordova-plugin-marketplace`), public. (It was previously on Bitbucket; some historical references may linger.)
