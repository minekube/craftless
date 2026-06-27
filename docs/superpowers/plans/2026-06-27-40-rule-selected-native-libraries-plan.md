# Rule-Selected Native Libraries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make daemon cache preparation handle Mojang rule-selected native
artifact libraries without putting selected native jars on the Java classpath.

**Architecture:** Keep the change in `daemon` cache preparation. This is
version/platform launch metadata handling, not runtime gameplay behavior.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization JSON, JUnit 5, Gradle through
mise.

---

### Task 1: Native Artifact Regression

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt`

- [x] **Step 1: Write the failing regression**

Add `cache preparation extracts rule selected native artifact libraries outside
classpath`. The fixture includes a normal LWJGL jar, a current-platform
`natives-*` artifact selected by Mojang rules, and a wrong-platform native
artifact.

- [x] **Step 2: Make the fixture CI-platform aware**

Derive the current test native classifier from `os.name` and `os.arch`, and
choose a wrong-platform artifact for negative evidence.

### Task 2: Metadata-Driven Native Classification

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`

- [x] **Step 1: Apply library rules**

Filter Minecraft libraries by Mojang `rules` before preparing classpath or
native artifacts.

- [x] **Step 2: Split native classifiers from classpath libraries**

Do not include artifact libraries with current-platform `natives-*`
classifiers in `minecraftLibraries()`.

- [x] **Step 3: Extract selected natives**

Include current-platform rule-selected native artifacts in
`minecraftNativeLibraries()` so existing native extraction and launch native
path behavior applies.

### Task 3: Verification

- [x] **Step 1: Run focused daemon tests**

Run:

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation extracts rule selected native artifact libraries outside classpath'
mise exec -- gradle :daemon:test
```

- [x] **Step 2: Run broad gates**

Run:

```sh
mise run lint
mise run architecture-check
mise run ci
git diff --check
```
