# Public Agent Blocked Outcome Design

## Intent

Do not enter the final Robin confirmation hold when the process-external
public-agent helper reports `BLOCKED`.

The 2026-06-27 Phase 57 rerun proved that child environment isolation preserved
server evidence: `server-evidence.jsonl` contained the Craftless client join and
`hello from Craftless final gameplay` chat. The same run exposed a separate
controller outcome bug. The public-agent helper printed
`publicAgentState=BLOCKED` with blocker
`insufficient-public-evidence:navigation.follow.succeeded`, but exited with
status `0`. `FabricClientSmokeController` only checked the process exit code,
then entered `waitForFinalGameplayConfirmation()` and wrote
`final-gameplay-ready.json`.

That ready artifact is misleading. A blocked public-agent run is not ready for
Robin acceptance and must not consume a human hold window.

## Product Rules

- Treat this as final-gameplay harness outcome propagation only.
- Do not change public gameplay API breadth.
- Do not add scenario shortcuts, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, static survival actions, or
  version support claims.
- Keep public-agent gameplay composed through discovered public actions and
  artifact evidence.
- Preserve Phase 57 child environment isolation.

## Required Behavior

After the public-agent helper process exits successfully:

1. inspect `public-agent-gameplay-results.jsonl` in the configured artifacts
   directory;
2. if any JSON line has `"event":"public-agent-blocked"`, fail the Fabric smoke
   controller with the blocker text and write `public-agent-blocked.json`;
3. do not write `final-gameplay-ready.json`;
4. do not run ready-notification commands;
5. do not enter the Robin confirmation hold.

The existing successful helper path must still enter the ready hold and write
`final-gameplay-ready.json`.

## Completion Gate

This phase is complete when:

- a focused failing test proves a blocked helper artifact writes
  `public-agent-blocked.json` and prevents ready notification;
- implementation passes the test by parsing helper artifact outcome after the
  helper exits;
- existing ready-notification and confirmation tests still pass;
- lint, architecture check, and full local CI pass;
- the checklist records the Phase 57 evidence-clearing fix separately from this
  Phase 58 outcome-propagation fix.
