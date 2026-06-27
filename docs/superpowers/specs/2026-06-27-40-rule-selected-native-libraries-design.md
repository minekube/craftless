# Phase 40: Rule-Selected Native Libraries Design

## Problem

Minecraft version metadata is not limited to the older `natives` classifier
shape. Some current libraries can appear as ordinary artifact downloads whose
Maven coordinate classifier is platform-native, with Mojang `rules` selecting
the operating system where that artifact applies.

If Craftless treats those native artifacts as ordinary classpath libraries, a
prepared client can miss the extracted native directory it needs or put native
jars on the Java classpath. That makes cache preparation brittle across newer
Minecraft versions and host platforms.

## Design

Cache preparation should classify Minecraft libraries from fetched metadata:

- apply Mojang library `rules` for the current platform before preparing a
  library;
- keep normal artifact libraries on the Java classpath;
- treat artifact libraries with `natives-*` Maven classifiers as native
  libraries when their rules and classifier match the current platform;
- continue supporting legacy `downloads.classifiers` native entries;
- extract selected native jars into native directories and include those
  directories in the launch native path;
- keep selected native jars out of the launch classpath.

This is supervisor cache/launch preparation only. It must not add public
gameplay APIs, generated action ids, route families, or CLI gameplay commands.

## Acceptance

- Focused daemon test proves a rule-selected native artifact is extracted into
  the native path and excluded from the classpath.
- The same test includes a wrong-platform native artifact that is not selected.
- The test is platform-aware so it passes on macOS, Linux, and Windows CI
  hosts.
- Existing cache preparation tests continue to pass.
