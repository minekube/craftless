# Craftwright

Real Minecraft client automation for tests, agents, and CI.

Craftwright is being designed as a CLI-first automation layer around a real
Minecraft Java client. The initial design is tracked in
`docs/superpowers/specs/2026-06-24-craftwright-design.md`.

The `mcw` CLI contract is tracked in
`docs/superpowers/specs/2026-06-24-mcw-cli-design.md`.

The first executable implementation plan is tracked in
`docs/superpowers/plans/2026-06-24-milestone-1-foundation.md`.

## Status

Craftwright is in early Milestone 1 development.

The first executable slice provides the `mcw` CLI, machine-readable output
contracts, project config, an in-memory engine, scenario parsing, and a stdio
daemon protocol. The real Minecraft client backend is the next milestone and
will attach behind the same engine interface.

## Development

```sh
go test ./... -count=1
```
