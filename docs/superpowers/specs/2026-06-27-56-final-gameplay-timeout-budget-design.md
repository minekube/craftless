# Final Gameplay Timeout Budget Design

## Intent

Make the final gameplay harness end with machine-readable confirmation or
timeout evidence instead of letting the outer local-smoke process kill the
client before the confirmation hold can finish.

The 2026-06-27 held run after Phase 55 proved the public-agent gameplay path:
it reached `publicAgentState=RAN`, wrote `final-gameplay-ready.json`, killed a
Sheep through generated `entity.attack`, and proved pickup through final
`inventory.query` containing `White Wool` and `Raw Mutton`. The run still
failed because the outer `LocalMinecraftServerSmoke` action timeout expired
while the Fabric controller was inside the human confirmation hold. No
`final-gameplay-confirmation-timeout.json` was written.

## Root Cause

`CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS` currently has two meanings in final
gameplay:

- the outer testkit timeout for the whole action command process;
- the inner Fabric controller timeout for the public-agent helper process.

The final Gradle task computes the outer timeout as
`fabricActionMillis + holdMillis + 180_000L`. That is insufficient when the
public-agent helper legitimately takes longer than `fabricActionMillis`, because
the ready hold starts only after the helper exits. In the failing run, the
public-agent path took about 31 minutes and the outer 35-minute timeout killed
the process only a few minutes into the 30-minute hold.

## Product Rules

- Do not add gameplay actions, routes, CLI gameplay commands, Fabric
  descriptor/binding pairs, survival macros, or scenario shortcuts.
- Keep this as internal final-gameplay evidence plumbing.
- Keep per-action HTTP request timeout separate from public-agent helper
  process timeout and separate from the outer local-smoke process timeout.
- The outer local-smoke timeout must cover public-agent helper process runtime,
  human confirmation hold, startup, connection, event collection, ready
  notification, cleanup, and a safety buffer.
- If Robin does not confirm in Minecraft chat before the hold deadline, the
  expected outcome is a successful final gameplay task with
  `final-gameplay-confirmation-timeout.json`, not a Gradle timeout failure.
- Goal completion still requires `final-gameplay-confirmation.json`.

## Timeout Model

Use explicit, non-circular budgets:

- `CRAFTLESS_PUBLIC_AGENT_ACTION_REQUEST_TIMEOUT_MS`: per generated HTTP action
  request made by the public-agent runner.
- `CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS`: Fabric controller request timeout
  for generated smoke actions.
- `CRAFTLESS_FABRIC_SMOKE_PUBLIC_AGENT_COMMAND_TIMEOUT_MS`: Fabric controller
  timeout for the external public-agent helper process.
- `CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS`: human confirmation hold
  window after ready evidence is written.
- `CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS`: outer testkit timeout for the entire
  Fabric client action process.
- `CRAFTLESS_LOCAL_SERVER_SMOKE_ACTION_TIMEOUT_MS`: optional operator override
  for the outer testkit timeout, still treated as a minimum.

## Completion Gate

This phase is complete only when:

- tests prove the final Gradle timeout calculation uses public-agent process
  timeout plus hold timeout plus buffer, not the shorter Fabric action timeout;
- tests prove `FabricClientSmokeController.fromEnvironment` accepts
  `CRAFTLESS_FABRIC_SMOKE_PUBLIC_AGENT_COMMAND_TIMEOUT_MS` and falls back
  compatibly when it is absent;
- held final gameplay is rerun and either writes
  `final-gameplay-confirmation.json` if Robin confirms or
  `final-gameplay-confirmation-timeout.json` if Robin does not;
- docs, AGENTS, and the project checklist record the outcome without claiming
  goal completion unless Robin confirmed in Minecraft chat.
