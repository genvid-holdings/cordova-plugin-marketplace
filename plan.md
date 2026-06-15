# Plan: Migrate CI/CD from CircleCI to GitHub Actions

## Branch
`ci/migrate-to-github-actions`

## Summary
Replace CircleCI's two tag-gated on-device build jobs with four GitHub Actions jobs
across two new per-platform workflow files (`android.yml`, `ios.yml`), each carrying:

- a **smoke** tier — emulator/simulator build on PR + push-to-main, **zero secrets**,
  proving the plugin compiles/links/loads;
- a **distribute** tier — signed artifact on `workflow_dispatch` + `v*.*.*` tags,
  secrets pulled from 1Password, uploaded as a downloadable artifact for **sideloading**
  (Android signed APK; iOS ad-hoc/Development-signed `.ipa`).

`ci.yml` (genvid-public-ci node-gate) and `publish.yml` (npm OIDC publish on tags) are
**unchanged**. `.circleci/config.yml` is deleted only after both smoke jobs are green in
live CI.

## Locked design decisions (rationale)
- **No paramedic.** Automated `defineAutoTests` is 3 specs; on a bare Android emulator
  `getInitiatingPackageName()` is null and `org.json.put(key,null)` drops the key, so
  spec 3 (`typeof name === 'string'`) fails. Paramedic-as-gate would require an
  out-of-scope native contract change. Smoke = compile-and-launch (what CircleCI did).
- **Simulator/emulator for smoke** → no signing, no certs, no `build.json`, no secrets.
- **iOS distribution = ad-hoc/Development-signed `.ipa` artifact, NOT TestFlight.**
  TestFlight (App Store Connect upload + review) is a no-go across the plugin fleet.
  Symmetric with Android (build → signed artifact → tester sideloads).
- **Android distribution = signed APK artifact for sideload.** Play internal track is a
  deferred optional follow-up, not built in this pass.
- **Secrets via 1Password**, single GitHub secret `OP_SERVICE_ACCOUNT_TOKEN`. Cert/profile/
  keystore are file attachments with dynamic names, so the `op` CLI fetches files while
  `load-secrets-action` resolves scalars.

## Human prerequisites
1. **`OP_SERVICE_ACCOUNT_TOKEN`** GitHub Actions secret (service account scoped to the
   `Project-Burbank` 1Password vault). — *Done by user.*
2. **Register tester device UDIDs** in the `iOS Development Profile` before running the
   iOS distribute job — the profile is baked into the `.ipa` at signing time; unregistered
   devices reject install.

## Resolved 1Password references (Project-Burbank vault)
- `APPLE_DEVELOPMENT_TEAM` ← `op://Project-Burbank/Apple Distribution Certificate/team_id`
- iOS Development cert (default; what CircleCI used):
  - id ← `op://Project-Burbank/AppStore Development Certificate/id`
  - password ← `op://Project-Burbank/AppStore Development Certificate/password`
  - `.p12` file ← `op://Project-Burbank/AppStore Development Certificate/<id>.p12` (dynamic)
- intermediate CA ← `op://Project-Burbank/Developer ID Certification Authority/<id>.cer`
  (may be redundant — GH runners carry WWDR; import for parity, drop if fetch fails)
- iOS Development profile ← `op://Project-Burbank/iOS Development Profile/<filename>` (dynamic)
- Android keystore:
  - alias ← `op://Project-Burbank/Burbank App Signing Keystore/alias`
  - password ← `op://Project-Burbank/Burbank App Signing Keystore/password`
  - keystore file ← `op://Project-Burbank/Burbank App Signing Keystore/<filename>` (dynamic)

---

## Tasks (each = one commit)

### Task 1 — Add simulator/version-guard npm scripts — ts-implementer
Purely additive to `package.json` `scripts`; existing `--device` dev scripts untouched.
- `setup:demo:sim` = `npm run setup:demo:cordova` (skips `setup:demo:config` — no team needed)
- `setup:demo:sim:ios` = `cd demo && npx --prefix . cordova platform add ios`
  (no `ios-deploy`/`xcode` shim; add `xcode` back only if `platform add` fails)
- `build:demo:ios:sim` = `cd demo && npx --prefix . cordova build ios` (no `--device`/`--release`)
- `version-guard` = a `scripts/version-guard.js` (NOT inline `require` — XML can't be
  `require`d) that `fs.readFileSync`s `package.json`, `tests/package.json`, `demo/config.xml`,
  regex-extracts each version, exits non-zero on mismatch.

**Commit:** `Add simulator-only and version-guard npm scripts`

### Task 2 — Add android.yml (smoke + distribute) — ts-implementer
- **smoke**: `pull_request` + `push:branches:[main]`; `ubuntu-latest`, Node 22;
  checkout → `npm run version-guard` → `npm ci` → `npm run package` → `setup:demo:sim` →
  `setup:demo:android` → `reactivecircus/android-emulator-runner@v2` (API 34, x86_64,
  google_atd, two-phase AVD) running `npm run build:demo:android`. Zero secrets.
- **distribute**: `workflow_dispatch` + `push:tags:['v*.*.*']`; `ubuntu-latest`;
  `1Password/install-cli-action` + `load-secrets-action` (keystore scalars) →
  `op read` the dynamic keystore file → build a **signed release APK** via
  `cordova build android --release -- --keystore=… --storePassword=… --alias=… --password=…`
  (**APK, not AAB** — do not use `build:demo:android:release`) → `upload-artifact`
  `app-release.apk`.

**Commit:** `Add Android smoke and distribute CI jobs (GitHub Actions)`

### Task 3 — Add ios.yml (smoke + distribute) — ts-implementer
- **smoke**: `pull_request` + `push:branches:[main]`; `macos-15`, `maxim-lobanov/setup-xcode`
  (pin 16.x), Node 22; checkout → setup-xcode → version-guard → `npm ci` → `npm run package`
  → `setup:demo:sim` → `setup:demo:sim:ios` → `build:demo:ios:sim`. Zero secrets, no build.json.
- **distribute**: `workflow_dispatch` + `push:tags:['v*.*.*']`; `macos-15`;
  install-cli + load-secrets (scalars) → `op read` dynamic `.cer`/`.p12`/profile files →
  `security` keychain import (mirror CircleCI `ios-import-certs`) → install provisioning
  profile → `setup:demo:sim` + `setup:demo:config` + `setup:demo:ios` →
  `npm run build:demo:ios:release` (archive) → `xcodebuild -exportArchive` with an
  `exportOptions.plist` (method `development`, team, profile) → `upload-artifact` the `.ipa`.
  (Fastlane `gym` is the fallback if `xcodebuild -exportArchive` signing proves brittle.)

**Commit:** `Add iOS smoke and distribute CI jobs (GitHub Actions)`

### Gate (no commit) — live-CI verification
Push branch → draft PR → both **smoke** jobs green. Then `workflow_dispatch` both
**distribute** jobs (NOT a tag — a tag also fires `publish.yml`/npm publish):
confirm `op` fetches the dynamic files, signing succeeds, APK and `.ipa` artifacts upload,
`.ipa` installs on a registered device. Only then proceed to Task 4.

### Task 4 — Delete CircleCI config — ts-implementer
Delete `.circleci/config.yml` (after the gate passes).

**Commit:** `Remove CircleCI config (replaced by GitHub Actions android.yml + ios.yml)`

### Task 5 — Update README — tech-writer
Replace CircleCI references with the GitHub Actions tiers; fix the 3 ghost scripts
(`install-sdk:android:windows`, `run:demo:<platform>`, `refresh:demo:<platform>`).

**Commit:** `Update README: replace CircleCI references with GitHub Actions, fix ghost scripts`

### Task 6 — Update CLAUDE.md — tech-writer
CI/Publishing + Platform notes: document the smoke/distribute tiers, single
`OP_SERVICE_ACCOUNT_TOKEN` secret, the `setup:demo:sim`/`setup:demo:config` split.

**Commit:** `Update CLAUDE.md: document GitHub Actions tiers, remove CircleCI references`

---

## Risks
- **AAB vs APK**: Android distribute must build an APK directly, not the existing
  `--packageType=bundle` script (AAB can't be `adb install`ed).
- **Dynamic 1Password files**: `load-secrets-action` resolves scalars only; the
  cert/profile/keystore files need `op read`. Service account must have `Project-Burbank` read.
- **UDID registration**: iOS `.ipa` installs only on devices registered in the profile.
- **`.xcarchive` path / Xcode 16.x on macos-15**: confirm on first run (add an `ls` step).
- **`setup:demo:sim:ios` shim**: add `xcode` npm pkg back if `cordova platform add ios` fails.
- **Tags fire publish.yml too**: verify distribute via `workflow_dispatch` only until first
  intentional release.

## Verification needs live CI
Emulator, simulator, and signing only exist on GH runners — Tasks 2/3 are written and
lint-checked locally but proven only by pushing the branch. The CircleCI deletion (Task 4)
is gated on both smoke jobs being green.
