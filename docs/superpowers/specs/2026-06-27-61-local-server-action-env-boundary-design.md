# Local Server Action Environment Boundary Design

## Intent

Phase 60 exposed a final-gameplay harness regression at the process boundary
between the local Minecraft server smoke owner and its configured action
command. The owner must prevent recursive final-gameplay/local-server ownership,
but the action command is also the Fabric smoke client child. Scrubbing all
smoke variables removes the child role and launches a normal Minecraft client
that never writes Craftless ready or confirmation artifacts.

## Product Rules

- Keep the public API surface unchanged.
- Do not add gameplay actions, descriptors, generated route families, CLI
  gameplay catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not add Minecraft version support or public version-specific APIs.
- Strip outer local-server lifecycle ownership variables from configured action
  commands: local-server smoke ownership, server setup, provisioning, and
  action-command recursion.
- Preserve final-gameplay, child Fabric smoke controller, and public-agent
  command variables for the configured action command because that command is
  the Fabric client role in final gameplay and still needs final-gameplay
  pathfinder/hold/confirmation context.
- Reinject only the child values owned by the local server boundary: server
  port, artifacts directory, and selected Java executable.

## Evidence

Tests and final artifacts must show:

- `CRAFTLESS_LOCAL_SERVER_SMOKE`, `CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT`, and
  `CRAFTLESS_SMOKE_ACTION_COMMAND_JSON` are removed before starting the action
  command;
- `CRAFTLESS_FINAL_GAMEPLAY`, `CRAFTLESS_FABRIC_CLIENT_SMOKE`,
  `CRAFTLESS_FABRIC_SMOKE_*`, and `CRAFTLESS_PUBLIC_AGENT_*` child settings
  survive for the action command;
- focused `:testkit:test` coverage passes;
- a rerun of final gameplay reaches the Fabric smoke controller and writes
  final-gameplay artifacts rather than opening a normal unmanaged client;
- no public gameplay API breadth is added.
