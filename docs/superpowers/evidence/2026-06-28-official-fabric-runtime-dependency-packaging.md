# Official Fabric Runtime Dependency Packaging Evidence

Date: 2026-06-28

## Scope

Phase 148 packages the latest/current official Fabric probe jar with the
shared Craftless runtime dependencies required for metadata-only self-attach.

This evidence does not claim Minecraft 26.x/latest support. The official lane
is still not added to the public driver manifest and still needs real launch,
self-attach, generated OpenAPI/actions/resources, SSE, packaged distribution,
and public API/CLI gameplay evidence.

## Red Evidence

Before implementation, the architecture guard failed:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane packages shared attach runtime dependencies without yarn remap gameplay lane'
```

Observed:

```text
FabricDriverModuleTest > official lane packages shared attach runtime dependencies without yarn remap gameplay lane() FAILED
```

## Green Evidence

Packaging guard:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane packages shared attach runtime dependencies without yarn remap gameplay lane'
```

Observed:

```text
BUILD SUCCESSFUL
```

Official jar build:

```sh
mise exec -- gradle :driver-fabric-official:compileKotlin :driver-fabric-official:processResources :driver-fabric-official:jar
```

Observed:

```text
BUILD SUCCESSFUL
```

Nested jars inspected:

```sh
jar tf driver-fabric-official/build/libs/driver-fabric-official-0.1.0-SNAPSHOT.jar | grep '^META-INF/jars/' | sort
```

Observed nested runtime jars include:

```text
META-INF/jars/driver-api-0.1.0-SNAPSHOT.jar
META-INF/jars/driver-fabric-attach-0.1.0-SNAPSHOT.jar
META-INF/jars/driver-runtime-0.1.0-SNAPSHOT.jar
META-INF/jars/kotlin-stdlib-2.4.0.jar
META-INF/jars/kotlinx-coroutines-core-jvm-1.11.0.jar
META-INF/jars/kotlinx-serialization-core-jvm-1.11.0.jar
META-INF/jars/kotlinx-serialization-json-jvm-1.11.0.jar
META-INF/jars/ktor-client-cio-jvm-3.5.0.jar
META-INF/jars/ktor-client-core-jvm-3.5.0.jar
META-INF/jars/ktor-server-cio-jvm-3.5.0.jar
META-INF/jars/ktor-server-core-jvm-3.5.0.jar
META-INF/jars/protocol-0.1.0-SNAPSHOT.jar
```

The observed nested jar list does not include `driver-fabric`,
`daemon`, or `bridge-hmc`.

Latest official lane probe:

```sh
mise run fabric-lane-check-latest-official
cat build/reports/fabric-lane-check-latest-official.status
```

Observed:

```text
status=compiled
```

Lint and whitespace:

```sh
mise exec -- gradle lint
git diff --check
```

Observed:

```text
BUILD SUCCESSFUL
```

## Guardrails

- No packaged 26.x driver manifest entry was added.
- No public gameplay action descriptor/catalog was added.
- No version-specific public route family was added.
- No survival shortcut was added.
- No final latest/current support claim was added.
