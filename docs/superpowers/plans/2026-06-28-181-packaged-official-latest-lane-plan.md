# Packaged Official Latest Lane Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Package the official Minecraft 26.2 lane into Craftless release archives so the packaged product can select a latest/current Fabric driver lane.

**Architecture:** Reuse the existing driver-lane catalog merge in `cli/build.gradle.kts`. Stage lane catalogs under `build/driver-lanes/older` and `build/driver-lanes/latest-official`, stage their artifacts under `build/driver-lanes/<distributionPath>`, then pass `build/driver-lanes` as the single extra-lane root to the CLI distribution task.

**Tech Stack:** Kotlin/JVM Gradle, Fabric Loom, mise, Bun Playwright distribution tests.

---

### Task 1: Red Distribution Guard

**Files:**
- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Write the failing test**

Add a test that reads `.mise.toml` and requires the package task to stage and
verify the latest official lane:

```ts
test("CLI distribution packages latest official fabric lane", () => {
  const mise = read(".mise.toml");

  expect(mise).toContain(":driver-fabric-official:jar");
  expect(mise).toContain("build/driver-lanes/latest-official");
  expect(mise).toContain("mods/fabric-26.2/craftless-driver-fabric-official.jar");
  expect(mise).toContain('"minecraftVersion": "26.2"');
  expect(mise).toContain('"fabricApiVersion": "0.153.0+26.2"');
  expect(mise).toContain('"javaMajorVersion": 25');
  expect(mise).toContain("java@temurin-25.0.3+9.0.LTS");
});
```

- [x] **Step 2: Verify red**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Expected: FAIL because `.mise.toml` does not yet stage
`build/driver-lanes/latest-official` or `craftless-driver-fabric-official.jar`.

### Task 2: Stage Official Lane In Package Task

**Files:**
- Modify: `.mise.toml`

- [x] **Step 1: Update cleanup and staging directories**

Change the package task from an older-only extra root to a shared root:

```toml
"rm -rf build/driver-lanes/older",
"rm -rf build/driver-lanes/latest-official",
"rm -rf build/driver-lanes/mods",
"mkdir -p build/driver-lanes/older build/driver-lanes/latest-official",
"mkdir -p build/driver-lanes/mods/fabric-1.20.6",
"mkdir -p build/driver-lanes/mods/fabric-26.2",
```

- [x] **Step 2: Keep older lane staging under the shared root**

Copy the older jar and catalog into:

```text
build/driver-lanes/mods/fabric-1.20.6/craftless-driver-fabric.jar
build/driver-lanes/older/fabric-driver-lanes.json
```

- [x] **Step 3: Build and stage the official jar**

Run the official module build through mise Java 25 and copy the non-sources jar:

```sh
mise exec java@temurin-25.0.3+9.0.LTS gradle@9.6.0 -- gradle :driver-fabric-official:jar
find driver-fabric-official/build/libs -maxdepth 1 -type f -name 'driver-fabric-official-*.jar' ! -name '*-sources.jar' -exec cp {} build/driver-lanes/mods/fabric-26.2/craftless-driver-fabric-official.jar \; -quit
```

- [x] **Step 4: Write official lane catalog**

Create `build/driver-lanes/latest-official/fabric-driver-lanes.json` with:

```json
{
  "entries": [
    {
      "loader": "FABRIC",
      "minecraftVersion": "26.2",
      "loaderVersion": "0.19.3",
      "path": "mods/fabric-26.2/craftless-driver-fabric-official.jar",
      "providerId": "fabric-26-2-official-lane",
      "artifactKey": "fabric-26-2-official-jar",
      "fabricApiVersion": "0.153.0+26.2",
      "javaMajorVersion": 25,
      "mappingsFingerprint": "craftless-fabric-official-26-2",
      "distributionPath": "mods/fabric-26.2/craftless-driver-fabric-official.jar"
    }
  ]
}
```

- [x] **Step 5: Pass the shared extra root to CLI distribution**

Update the CLI distribution command to:

```sh
mise exec -- gradle :cli:distZip :cli:distTar :driver-fabric:remapJar -Pcraftless.extraFabricDriverLaneRoot=build/driver-lanes
```

### Task 3: Verify Archives And Evidence

**Files:**
- Modify: `.mise.toml`
- Create: `docs/superpowers/evidence/2026-06-28-packaged-official-latest-lane.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`

- [x] **Step 1: Add archive checks**

Extend `mise run package-cli` with tar and zip checks for:

```sh
mods/fabric-26.2/craftless-driver-fabric-official.jar
"minecraftVersion": "26.2"
mods/fabric-26.2/craftless-driver-fabric-official.jar
"javaMajorVersion": 25
"mappingsFingerprint": "craftless-fabric-official-26-2"
```

- [x] **Step 2: Verify green**

Run:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
mise run package-cli
mise run fabric-lane-check-latest-official
git diff --check
```

Expected: all pass.

- [x] **Step 3: Record evidence**

Create `docs/superpowers/evidence/2026-06-28-packaged-official-latest-lane.md`
with the red test result, green commands, archive contents, and remaining
CL-03 blockers.

- [x] **Step 4: Update status docs**

Add Phase 181 to `docs/superpowers/phase-index.md`. In
`docs/project-completion-checklist.md`, mark packaging progress only where
proven; do not close CL-03, CL-03e.3, or CL-03f until connected packaged-lane
artifacts and public smoke exist.

- [ ] **Step 5: Commit and push**

Run:

```sh
git add .mise.toml playwright/src/distribution.test.ts docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-181-packaged-official-latest-lane-design.md docs/superpowers/plans/2026-06-28-181-packaged-official-latest-lane-plan.md docs/superpowers/evidence/2026-06-28-packaged-official-latest-lane.md
git commit -m "feat: package official latest fabric lane"
git push origin main
```

## Self-Review

- Spec coverage: the plan adds package-task guard coverage, official jar
  staging, official lane catalog rendering, archive checks, evidence, and
  status updates.
- Placeholder scan: no TBD/TODO/fill-in placeholders remain.
- Type consistency: paths use the same `build/driver-lanes` root and
  `mods/fabric-26.2/craftless-driver-fabric-official.jar` distribution path
  across tests, packaging, checks, and evidence.
