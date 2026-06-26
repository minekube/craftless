# Distribution Usability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add first-class release, install, Docker, GitHub Action, and README quickstart surfaces so external users can run Craftless without building from source.

**Architecture:** Keep release building in GitHub Actions through mise and Gradle. Package the existing JVM application distribution. Docker is a runtime image that copies the built CLI archive from the build context and downloads Minecraft artifacts only at runtime into `/var/lib/craftless`.

**Tech Stack:** Gradle Application plugin, GitHub Actions, GHCR Docker image, shell install script, Bun packaging tests, Java 21 runtime.

---

### Task 1: Distribution Contract Tests

**Files:**
- Create: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add tests for distribution files**

Write Bun tests that read repository files and assert:

```ts
expect(await fileExists(".github/workflows/release.yml")).toBe(true);
expect(await fileExists(".github/actions/setup-craftless/action.yml")).toBe(true);
expect(await fileExists("install.sh")).toBe(true);
expect(await fileExists("Dockerfile")).toBe(true);
```

Assert the Dockerfile does not contain `gradle`, `mise`, `npm`, `yarn`,
`pnpm`, or `bun`, and does contain `COPY build/docker/craftless/`.

- [x] **Step 2: Run RED tests**

Run:

```sh
mise exec -- bun test playwright
```

Expected: FAIL because the distribution files do not exist yet.

### Task 2: Release Workflow And Docker Context

**Files:**
- Create: `.github/workflows/release.yml`
- Modify: `.mise.toml`

- [x] **Step 1: Add release workflow**

Create a tag-triggered workflow that checks out code, installs mise tools,
runs `mise run ci`, runs `mise run package-cli`, writes checksums, creates a
GitHub Release, logs into GHCR, and builds/pushes Docker using the prepared
CLI distribution context.

- [x] **Step 2: Add mise packaging task**

Add `package-cli` to `.mise.toml`:

```toml
[tasks.package-cli]
description = "Build Craftless CLI release archives and Docker context"
run = [
    "rm -f cli/build/distributions/craftless-*.zip cli/build/distributions/craftless-*.tar",
    "mise exec -- gradle :cli:distZip :cli:distTar",
    "rm -rf build/docker/craftless",
    "mkdir -p build/docker/craftless",
    "tar -xf cli/build/distributions/craftless-*.tar -C build/docker/craftless --strip-components=1",
]
```

### Task 3: Runtime Docker Image

**Files:**
- Create: `Dockerfile`
- Create: `docker/entrypoint.sh`

- [x] **Step 1: Add Dockerfile**

Use a Java 21 runtime base. Install runtime libraries for headless clients,
copy `build/docker/craftless/` to `/opt/craftless`, expose `8080`, set
`CRAFTLESS_WORKSPACE=/var/lib/craftless`, and run an entrypoint.

- [x] **Step 2: Add entrypoint**

Default to:

```sh
exec /opt/craftless/bin/craftless server start --port "${CRAFTLESS_PORT:-8080}" --workspace "${CRAFTLESS_WORKSPACE:-/var/lib/craftless}"
```

If arguments are passed, execute them as the container command.

### Task 4: Install Script And Setup Action

**Files:**
- Create: `install.sh`
- Create: `.github/actions/setup-craftless/action.yml`

- [x] **Step 1: Add install script**

Download a release archive from `https://github.com/minekube/craftless`.
Install `craftless` to `${CRAFTLESS_INSTALL_DIR:-$HOME/.local/bin}`. Support
`CRAFTLESS_VERSION`, `CRAFTLESS_REPOSITORY`, and `CRAFTLESS_INSTALL_DIR`.

- [x] **Step 2: Add composite action**

The action accepts `version` and `start` inputs, installs the CLI through
`install.sh`, optionally starts the daemon in the background, waits for
`/openapi.json`, and exposes `api-url`.

### Task 5: README And Checklist

**Files:**
- Modify: `README.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Add quickstart sections**

Place install, Docker, and GitHub Actions quickstarts near the top of README.
Make clear that Docker does not bundle Minecraft artifacts.

- [x] **Step 2: Add Phase 25 checklist**

Record release/install/action/Docker status and verification commands.

### Task 6: Verify, Commit, Push

**Files:**
- All files above

- [x] **Step 1: Run verification**

Run:

```sh
git diff --check
mise exec -- bun test playwright
mise run package-cli
docker build -t craftless:local .
docker run --rm craftless:local /opt/craftless/bin/craftless server start --once --port 0 --workspace /tmp/craftless
mise run ci
```

If Docker is unavailable locally, report that specifically and keep the
Dockerfile covered by static tests.

- [ ] **Step 2: Commit and push**

Commit with:

```sh
git add .github/actions/setup-craftless/action.yml .github/workflows/release.yml .mise.toml Dockerfile docker/entrypoint.sh install.sh README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-25-distribution-usability-design.md docs/superpowers/plans/2026-06-26-25-distribution-usability-plan.md playwright/src/distribution.test.ts
git commit -m "feat: add distribution surfaces"
git push origin main
```
