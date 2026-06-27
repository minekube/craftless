# Phase 39: Fabric Library Replacement Design

## Problem

Fabric loader profiles can supply libraries that intentionally supersede
libraries listed in the Minecraft version manifest. When Craftless prepares a
Fabric client, keeping both copies on the launch classpath makes the launcher
more version-specific than it should be and can produce classpath conflicts as
Minecraft, Fabric Loader, and transitive libraries move at different speeds.

The fix belongs in supervisor cache preparation and launch planning. It is not
a driver action, generated API action, CLI gameplay command, or public route.

## Design

During Fabric cache preparation, compare Minecraft libraries and Fabric loader
profile libraries by Maven module identity: group plus artifact. If Fabric
provides the same module, Fabric's library wins for the prepared launch plan.

The replacement rule should:

- keep Fabric libraries from the loader profile on the launch classpath;
- remove replaced Minecraft libraries from prepared artifacts and launch
  classpath;
- keep unrelated Minecraft libraries such as authentication and game support
  libraries;
- use metadata from version manifests and Fabric loader profiles, not a
  hard-coded Minecraft or Fabric version list;
- leave Minecraft native libraries, assets, client jars, Java runtime files,
  and loader metadata unaffected.

## Acceptance

- Focused daemon test proves Fabric `org.ow2.asm:asm` replaces Minecraft
  `org.ow2.asm:asm` while unrelated Minecraft libraries remain cached and on
  the launch classpath.
- The implementation derives replacement from Maven coordinates in fetched
  metadata rather than hard-coded URLs or versions.
- No public API, generated gameplay action id, static route family, or CLI
  gameplay command is added.
