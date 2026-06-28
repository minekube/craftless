# Latest Official Mapping Lane Probe Evidence

Phase 145 adds an executable latest/current Fabric lane probe for the
official/Mojang-mapping boundary. It does not add Minecraft 26.x support.

## References

- Mojang version manifest:
  `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`
- Fabric porting docs:
  `https://docs.fabricmc.net/develop/porting/`
- Fabric mappings migration docs:
  `https://docs.fabricmc.net/develop/porting/mappings/`

## TDD Record

Red command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric compiled lane build is parameterized for compatibility probes' --tests '*FabricDriverModuleTest.mise latest lane probe uses official mapping boundary not yarn remap lane*'
```

Observed red result before implementation:

```text
FabricDriverModuleTest > fabric compiled lane build is parameterized for compatibility probes() FAILED
FabricDriverModuleTest > mise latest lane probe uses official mapping boundary not yarn remap lane() FAILED
```

Green command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric compiled lane build is parameterized for compatibility probes' --tests '*FabricDriverModuleTest.mise latest lane probe uses official mapping boundary not yarn remap lane*'
```

Observed green result:

```text
BUILD SUCCESSFUL in 16s
17 actionable tasks: 17 up-to-date
```

## Probe Command

```sh
mise run fabric-lane-check-latest-official
```

The task runs Gradle under Java 25 through mise:

```sh
mise exec java@temurin-25.0.3+9.0.LTS gradle@9.6.0 -- gradle ...
```

It passes the latest/current lane identity:

- Minecraft `26.2`;
- Fabric Loader `0.19.3`;
- Fabric API `0.153.0+26.2`;
- Java major version `25`;
- mapping mode `official`;
- lane id `fabric-latest-official-lane`;
- mappings fingerprint `craftless-fabric-official-bindings-26-2`.

## Probe Result

Status artifact:

```text
status=source-compatibility-blocked
blockers=loom-remap-requires-mappings
```

Log excerpt:

```text
Failed to setup Minecraft, java.lang.IllegalArgumentException: Configuration 'mappings' has no dependencies
```

Fresh whitespace verification:

```sh
git diff --check
```

Observed result: exit code 0 with no output.

This proves the repository can now run a latest/current probe under the needed
Java runtime, and the next implementation blocker is the current
`fabric-loom-remap` build boundary. Latest/current support requires a
non-remap official Fabric lane module or equivalent build boundary before a
26.x driver artifact can compile, package, launch, attach, expose generated
OpenAPI/actions/resources, stream SSE, and pass public API/CLI gameplay
verification.
