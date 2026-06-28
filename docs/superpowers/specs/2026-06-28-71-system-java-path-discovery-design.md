# System Java PATH Discovery Design

## Problem

Phase 27 made Java runtime selection a supervisor/runtime concern with
configured, managed-cache, mise, and system providers. The system provider
looked at `JAVA_HOME` and explicit injected candidates, but it did not discover
`java` from `PATH`.

That leaves normal product installations weaker than intended: a compatible
system Java can be installed and visible to a user shell, while Craftless still
reports `java-runtime.unsatisfied` unless the runtime is managed, configured,
or installed under mise.

## Design

Keep this entirely inside the supervisor/runtime Java resolver. The system
provider should scan `PATH` entries for `java` and `java.exe`, append those
candidates after `JAVA_HOME` and before injected test candidates, and let the
existing bounded `ProcessBuilder(..., "-version")` validator decide whether a
candidate is usable.

This is not a repository tooling change. Repository commands still run through
`mise`, and tests use fake Java executables in temporary directories.

## Boundaries

- Do not add gameplay actions, descriptors, generated route families, CLI
  gameplay catalogs, Fabric bindings, scenario shortcuts, or Minecraft version
  support claims.
- Do not execute shell commands or depend on shell lookup semantics.
- Do not require `mise` to exist on `PATH` for product runtime launches.
- Keep public evidence Craftless-owned: provider `SYSTEM`, selected/rejected
  Java descriptors, and machine-readable reasons.

## Verification

- A failing test proves a fake Java 25 executable on `PATH` is initially not
  selected when `JAVA_HOME` is absent.
- The same test passes after `SystemJavaRuntimeProvider` discovers `PATH`
  candidates.
- Full daemon tests, architecture checks, and CI verify the change stays within
  supervisor/runtime behavior.
