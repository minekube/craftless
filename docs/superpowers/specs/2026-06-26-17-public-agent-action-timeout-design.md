# Public Agent Action Timeout Design

## Intent

Make the external public-agent runner fail in a controlled, observable way when
a generated action request hangs or times out. The final gameplay harness must
not lose artifacts or fail only because the agent process crashed before
recording the blocker.

The latest live run reached the server, wrote chat, and then the external
public-agent command failed with a Ktor request timeout during
`POST /clients/{id}:run`. The durable fix is not a retry loop. Generated action
invocations can be state-changing, so a timeout leaves outcome ambiguity. The
runner should record that ambiguity and stop.

## Product Rules

- Do not retry non-idempotent generated actions by default.
- Do not add scenario actions to avoid slow generic actions.
- Preserve action logs and state artifacts before returning.
- Keep blockers machine-readable.
- Keep public-agent completion evidence separate from smoke-controller
  diagnostic actions.

## Behavior

When `POST /clients/{id}:run` fails or times out:

1. Append a `public-agent-action` artifact line for the attempted action.
2. Store a JSON response payload with `status = "FAILED"` and a stable blocker.
3. Return `PublicAgentGameplayState.BLOCKED`.
4. Set the blocker to `action-request-failed:<action-id>`.
5. Write normal state and gameplay artifact files.
6. Let the process exit normally after printing `publicAgentState=BLOCKED`.

No automatic retry is added in this phase because actions such as navigation,
break, inventory changes, and combat may already have taken effect.

## Evidence

Tests and live artifacts must show:

- an injected action request failure becomes a blocked result;
- the attempted action appears in `public-agent-gameplay-results.jsonl`;
- the blocker appears in the artifacts and command output;
- successful paths are unchanged;
- no scenario shortcut strings are introduced.

