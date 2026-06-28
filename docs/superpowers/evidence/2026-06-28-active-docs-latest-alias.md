# Active Docs Latest Alias Evidence

Phase: 114

## Live Manifest Check

Command:

```sh
mise exec -- bun -e 'const manifest = await (await fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")).json(); console.log(JSON.stringify({latest: manifest.latest, firstRelease: manifest.versions.find(v => v.type === "release"), firstSnapshot: manifest.versions.find(v => v.type === "snapshot")}, null, 2));'
```

Result:

- Exit code: 0
- Mojang latest release at verification time: `26.2`.
- Mojang latest snapshot at verification time: `26.3-snapshot-1`.
- Active docs still prefer aliases because those concrete values drift.

## Red

Command:

```sh
mise exec -- bun test playwright/src/distribution.test.ts --test-name-pattern "active docs prefer latest aliases over concrete latest ids"
```

Result:

- Exit code: 1
- Failure reason: README did not contain `"version": "latest-release"` and
  still used concrete `1.21.6` in the active create-client example.

## Green

Command:

```sh
mise exec -- bun test playwright/src/distribution.test.ts --test-name-pattern "active docs prefer latest aliases over concrete latest ids"
```

Result:

- Exit code: 0
- Bun reported 1 passing focused docs test with 6 expectations.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Results:

- `git diff --check`: exit code 0.
- `mise run ci`: exit code 0.
- `mise run ci` completed:
  - `mise exec -- gradle lint`
  - `mise run unused-check`
  - `mise exec -- gradle test`
  - `mise exec -- bun test playwright`
- Bun reported 18 passing tests across the Playwright helper suite, including
  the new active-docs alias guard.

## Scope Guard

This phase only updates active docs and docs tests so users see alias-first
current-version flows.

It adds no compiled Fabric lane, public gameplay action, generated route
family, CLI gameplay catalog, Fabric gameplay binding, scenario shortcut,
public version-specific API, runnable latest/older lane, or new Minecraft
support claim.
