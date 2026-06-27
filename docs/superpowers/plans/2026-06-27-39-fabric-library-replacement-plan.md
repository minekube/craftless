# Fabric Library Replacement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Make Fabric cache preparation replace duplicate Minecraft libraries
by Maven module identity so prepared launch classpaths remain version-aware and
loader-profile driven.

**Architecture:** Keep the change in `daemon` supervisor cache preparation.
This is launch artifact hygiene, not runtime gameplay behavior.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization JSON, JUnit 5, Gradle through
mise.

---

### Task 1: Replacement Regression

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt`

- [x] **Step 1: Write the failing regression**

Add `fabric cache preparation lets fabric libraries replace duplicate minecraft
libraries`. The fixture uses a Minecraft version manifest with
`org.ow2.asm:asm:9.6` and `com.mojang:authlib`, plus a Fabric loader profile
with `org.ow2.asm:asm:9.10.1` and Fabric Loader.

- [x] **Step 2: Verify RED**

Run:

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.fabric cache preparation lets fabric libraries replace duplicate minecraft libraries'
```

Expected before implementation: fails because both Minecraft ASM and Fabric
ASM appear in the prepared launch classpath.

### Task 2: Metadata-Driven Replacement

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`

- [x] **Step 1: Parse module keys**

Derive a private Maven module key from manifest/library `name` values using
group plus artifact.

- [x] **Step 2: Filter replaced Minecraft libraries**

Before constructing prepared artifacts and the launch plan, remove Minecraft
libraries whose module key is present in Fabric loader-profile libraries.

- [x] **Step 3: Fetch only effective launch libraries**

Write fetched bytes only for effective Minecraft libraries and all Fabric
libraries. Leave non-library artifacts unchanged.

### Task 3: Verification

- [x] **Step 1: Verify GREEN**

Run:

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.fabric cache preparation lets fabric libraries replace duplicate minecraft libraries'
```

Expected: pass.

- [x] **Step 2: Run broader gates**

Run:

```sh
mise exec -- gradle :daemon:test
mise run lint
mise run architecture-check
mise run ci
git diff --check
```

Expected: all pass.
