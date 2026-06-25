# CLI Module Instructions

`cli/` owns the JVM `craftless` command-line interface.

## Scope

- Static CLI core: daemon startup/discovery, config, auth/token selection,
  output modes, error rendering, and generic dispatch.
- Adaptive command/help dispatch from kernel and per-client OpenAPI/action
  descriptors.

## Rules

- Use Clikt for the JVM CLI and Mordant for terminal output.
- Keep action commands adaptive. Do not generate Kotlin source or add static
  commands for every Minecraft action.
- Static commands should cover setup, daemon lifecycle, client creation/listing,
  OpenAPI/action discovery, and generic action invocation:
  `craftless clients <id> run <action> ...`.
- Dynamic aliases such as `craftless clients <id> player move --forward` and their
  `--help` output must come from `/clients/{id}/openapi.json` and
  `/clients/{id}/actions`.
- Preserve stdout for primary data, stderr for diagnostics/progress, stable
  `--json`/`--jsonl` output, explicit exit codes, and non-interactive
  `--no-input` behavior.
- Use Ktor Client for daemon/API calls.

## Verification

```sh
mise exec -- gradle :cli:test
```
