# Latest Driver Lane Preflight Evidence

Phase 144 makes missing packaged latest/current Fabric driver lanes fail before
heavyweight binary cache preparation.

## TDD Record

Red test command:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.client creation rejects missing latest fabric driver lane before binary downloads'
```

Observed red behavior before implementation: the new test failed because the
create-client path fetched binary artifacts before the missing `26.2` driver
manifest lane was rejected.

Green test command after implementation:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.client creation rejects missing latest fabric driver lane before binary downloads'
```

Observed green behavior after implementation: the focused test passed.

## Expected Behavior

The daemon resolves `latest-release` to concrete runtime identity data before
full cache preparation:

- Minecraft `26.2`;
- Fabric Loader `0.19.3`;
- Fabric API `0.153.0+26.2`;
- Java major version `25`.

When the packaged driver-mod manifest has no matching entry, `/clients` returns
HTTP 400 with that resolved identity and does not launch a client, fetch binary
artifacts, or create asset-object cache contents.

## Verification

Focused regression coverage passed:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.client creation rejects missing latest fabric driver lane before binary downloads' --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest' --tests '*ConfiguredClientRuntimeDriverModProviderTest*'
```

Observed result:

```text
BUILD SUCCESSFUL in 1s
14 actionable tasks: 1 executed, 13 up-to-date
```

Whitespace verification passed:

```sh
git diff --check
```

Observed result: exit code 0 with no output.

This phase does not add latest/current support. It only makes the unsupported
lane failure earlier and more useful.
