# Plan: CircleCI packages job → GitHub Actions + npm OIDC publishing

Replace the CircleCI `packages` job (lint → `npm pack` → **Azure blob upload**) with the
shared **`genvid-holdings/genvid-public-ci`** GitHub Actions recipe: lint/typecheck/test/build
gate on PRs, and **OIDC trusted-publishing** of `@genvid/cordova-plugin-marketplace` to
npmjs.com on a `v*.*.*` tag (no stored npm token, automatic provenance). Migrate all
Bitbucket → GitHub metadata. Keep the android/ios device-build jobs on CircleCI, decoupled
from the removed `packages` job.

Branch: `ci/github-actions-npm-publish`

## Decisions

| Decision | Choice | Why |
|---|---|---|
| Migration scope | Replace `packages` job only; keep android/ios on CircleCI, decoupled | android/ios are device-build *test* jobs (macOS/Android runners, Apple signing, OnePassword) — out of scope for the publishing change |
| npm name | `@genvid/cordova-plugin-marketplace` (scoped) | Unscoped `cordova-plugin-marketplace` is owned by an unrelated package (Xmartlabs, 2017) — publishing under it is impossible |
| Plugin id | Scope `plugin.xml` `id` + `cordova.id` to the npm name too | Standard for scoped Cordova plugins; keeps npm name and plugin id aligned |
| Tests fixture | **Not** published to npm; stays a locally-packed CI input | Latest official `cordova-plugin-device` (3.0.0) drops `tests/` from its tarball; this repo's `.npmignore` already excludes it. The android/ios jobs pack it on demand via `npm run package` |
| First version | `1.1.0` | Marks the npm/OIDC era, distinct from the 1.0.x Azure-era tags |
| typecheck | Real `tsc --noEmit` over `types/index.d.ts` | Cheaply validates the hand-maintained public TS surface |

## Background facts (verified)

- Repo is **already** `github.com/genvid-holdings/cordova-plugin-marketplace`, **public**
  (CLAUDE.md's "Bitbucket" note + `.genvid-agent.json` `repo.host: bitbucket` are stale).
  Same org as `genvid-public-ci`, so the OIDC recipe applies directly.
- The shared **node-gate** runs `npm ci → lint → typecheck → test → build` (all
  unconditional) + a non-failing `npm publish --dry-run`. The templates are drop-in /
  zero-edit; the trusted-publisher registration matches on the `publish.yml` filename.
- `publish.yml` runs `npm run build` then `npm publish --provenance --access public` —
  publishes the **root package only**.
- The demo's `demo/config.xml` (tracked) hand-pins the tarball filenames by version
  (lines 12–13). The scoped pack flattens `@genvid/` → `genvid-`, so the plugin tarball
  becomes `genvid-cordova-plugin-marketplace-1.1.0.tgz`.
- Old git tags: `1.0.0`, `1.0.1` (no `v`). The recipe triggers on `v*.*.*`, so the new
  convention is `v1.1.0`. CircleCI's android/ios tag filter must adopt the `v` prefix.

## The gate-script wrinkle

The plugin has no compilation, but the gate requires four scripts. Each no-op is honest
and documented:

- `lint` → existing `eslint .`
- `typecheck` → **real** `tsc --noEmit` over `types/index.d.ts` (new `tsconfig.json` +
  `typescript` devDep)
- `test` → honest no-op (`echo` + exit 0); the Jasmine specs are **device-only**
- `build` → honest no-op (Cordova plugin ships sources as-is). Kept **separate** from the
  existing `package` script, which still packs tarballs for the device-build jobs.

## Tasks (one commit each; prep before automation)

### Prep — make it publishable

**Task 1 — Scope the package and point metadata at GitHub.**
- `package.json`: `name` → `@genvid/cordova-plugin-marketplace`; `version` → `1.1.0`;
  `cordova.id` → scoped name; `repository` → `{ "type": "git", "url":
  "https://github.com/genvid-holdings/cordova-plugin-marketplace.git" }`; `bugs` →
  `https://github.com/genvid-holdings/cordova-plugin-marketplace/issues`; add
  `"publishConfig": { "access": "public" }`.
- `plugin.xml`: `id` → scoped name; `version` → `1.1.0`; `<repo>` / `<issue>` → GitHub URLs.
- `tests/package.json`: `version` → `1.1.0`.
- Validate: implementer confirms `cordova plugin add` still resolves the scoped id.

**Task 2 — Add CI gate scripts.**
- `package.json` scripts: `"typecheck": "tsc --noEmit"`, `"test": "echo \"no host-side
  unit tests (Cordova specs are device-only)\""`, `"build": "echo \"no build step
  (Cordova plugin ships sources as-is)\""`.
- Add `tsconfig.json` (`noEmit`, `strict`, `skipLibCheck`, `files: ["types/index.d.ts"]`).
- Add `typescript` to `devDependencies`; run `npm install` to refresh `package-lock.json`.
- Validate: `npm run lint && npm run typecheck && npm run test && npm run build` all green.

### Automation

**Task 3 — Add GitHub Actions workflows.**
- `.github/workflows/ci.yml` and `.github/workflows/publish.yml` **verbatim** from the
  fetched genvid-public-ci templates (no per-package edits; keep the exact `publish.yml`
  filename).

**Task 4 — Drop Azure upload; decouple CircleCI device jobs.**
- `.circleci/config.yml`: delete the `packages` job, the `az-login` command, and the
  `azure-cli` orb + `azure-cli/install`/`az-login` steps from android/ios.
- android/ios: remove `requires: packages` + `attach_workspace` + `cp packages/*.tgz .`;
  add a `npm run package` step (produces both tarballs locally) then `cp *.tgz .`.
- Update tag filters `/\d+\..*/` → `/v\d+\..*/`; bump `plugin_version` param to `1.1.0`.

**Task 5 — Update demo plugin references.**
- `demo/config.xml` lines 12–13 → `file://../genvid-cordova-plugin-marketplace-1.1.0.tgz`
  and `file://../cordova-plugin-marketplace-tests-1.1.0.tgz`.

**Task 6 — Docs.**
- `README.md`: scoped install (`cordova plugin add @genvid/cordova-plugin-marketplace`);
  fix stale `cordova-plugin-template` naming; note GitHub Actions + npm publishing replacing
  CircleCI/Azure.
- `CLAUDE.md`: Bitbucket → GitHub; `packages` job → GitHub Actions publishing; new `v*.*.*`
  tag convention.
- `.genvid-agent.json`: `repo.host` → `github`; add `commands.typecheck` / `commands.test`.

## Human-only hand-off (Phase 5 — not part of the plan commits)

1. **One-time npm bootstrap** (user, blocking): claim `@genvid/cordova-plugin-marketplace`
   with a short-lived granular token (`npm version 1.1.0-bootstrap.0 --no-git-tag-version`
   → `npm publish --access public` → `npm deprecate` → restore version), then register the
   GitHub trusted publisher on npmjs.com (org `genvid-holdings`, repo
   `cordova-plugin-marketplace`, workflow `publish.yml`, environment blank). **Revoke the
   token.**
2. `git tag v1.1.0 && git push origin v1.1.0` → `publish.yml` gates, enforces tag↔version
   equality, publishes with `--provenance`.
3. Verify the provenance badge on the npmjs.com package page.

## Validation gates

- `genvid-dev:validator` after each task (`npm run lint`, plus the new gate scripts).
- `genvid-dev:code-reviewer` at the end; `genvid-dev:tech-writer` if it flags doc gaps.

## Risks

- **Scoped rename ripples**: packed tarball filename changes → demo `config.xml` (Task 5)
  and the device jobs' `cp *.tgz .` must agree. Verify `cordova plugin add` resolves the
  scoped id (Task 1).
- **Tag convention shift** (`1.0.1` → `v1.1.0`): both `publish.yml` and the CircleCI
  android/ios filters depend on it — keep them consistent (Task 4).
- **No LICENSE file at root** (license is MIT-0 in metadata only): npm publish still works;
  adding a `LICENSE` file is optional and out of scope.
- **Bootstrap is human-only**: OIDC cannot perform a name's first publish; the release is
  blocked on the user's npm-account step.
