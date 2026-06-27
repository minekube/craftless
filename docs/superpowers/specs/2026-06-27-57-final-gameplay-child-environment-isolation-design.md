# Final Gameplay Child Environment Isolation Design

## Intent

Prevent final gameplay subprocesses from inheriting smoke-owner environment
variables that can make a child process behave like a second local server
owner.

The 2026-06-27 held rerun after Phase 56 reached the ready window on
`127.0.0.1:53413`: the Craftless-controlled client joined, sent
`hello from Craftless final gameplay`, used generated public actions, earned a
`Wooden Sword`, killed a Pig through `entity.attack`, proved `Raw Porkchop` in
`inventory.query`, and wrote `final-gameplay-confirmation-timeout.json` after
Robin did not confirm in Minecraft chat.

The Gradle task still failed afterward because `server-evidence.jsonl`
contained only the disconnect event. The combined server log showed the expected
join/chat evidence, plus a second server start attempt in the same root that
failed on `world/session.lock`. That second owner path cleared the shared
artifacts directory while the first server was still running.

## Root Cause

Fabric final gameplay launches nested subprocesses:

- the visible Fabric client action command;
- the process-external public-agent helper command;
- the ready-notification command.

The visible Fabric client needs smoke/final-gameplay environment to run the
in-client smoke controller. The child helper commands launched from that
controller do not own the local server. If they inherit
`CRAFTLESS_FABRIC_CLIENT_SMOKE`, `CRAFTLESS_FINAL_GAMEPLAY`,
`CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT`, `CRAFTLESS_SMOKE_ACTION_COMMAND_JSON`, and
related owner variables, nested Gradle configuration or helper code can treat
the child as another smoke owner. That can launch a second server or clear
shared artifacts, invalidating otherwise correct evidence.

## Product Rules

- Keep this as harness/environment isolation only.
- Do not change public gameplay API breadth.
- Do not add scenario shortcuts, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, or version support claims.
- Preserve explicit child-specific variables:
  `CRAFTLESS_PUBLIC_AGENT_BASE_URL`, `CRAFTLESS_PUBLIC_AGENT_CLIENT_ID`,
  `CRAFTLESS_PUBLIC_AGENT_ARTIFACTS_DIR`,
  `CRAFTLESS_PUBLIC_AGENT_ACTION_REQUEST_TIMEOUT_MS`, and
  `CRAFTLESS_FABRIC_SMOKE_READY_*`.
- Strip inherited local-server/final-gameplay owner variables before adding the
  child-specific variables.

## Completion Gate

This phase is complete only when:

- focused tests prove the smoke child environment sanitizer removes inherited
  server/final-gameplay owner variables and preserves unrelated environment;
- public-agent and ready-notification subprocess paths apply the sanitizer;
- focused controller subprocess tests pass;
- final gameplay is rerun and no longer fails with missing expected chat
  evidence after reaching the ready/timeout gate;
- docs, AGENTS, and the project checklist record the outcome truthfully.
