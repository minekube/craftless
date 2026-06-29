# CL-07 Final Public Gameplay Evidence

Gate: CL-07 final honest public gameplay.

Result: closed for this gate only. CL-08 publish completion and the overall
project goal remain open until the final state is committed, pushed, clean, and
indexed.

## Command And Result

| Command | Result |
| --- | --- |
| `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime graph exposes block query from client state*'` | exit `0`; radius-64 block query regression passed. |
| `mise exec -- bun test playwright/src/distribution.test.ts --filter "final public gameplay probe uses generated public surfaces only"` | exit `0`; final probe guard passed with `179` expectations. |
| `bash -n scripts/final-public-gameplay-probe.sh` | exit `0`. |
| `git diff --check -- scripts/final-public-gameplay-probe.sh playwright/src/distribution.test.ts driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt docs/project-completion-checklist.md` | exit `0`, no output. |
| `mise run final-public-gameplay-probe` | exit `0`; `BUILD SUCCESSFUL in 2m 50s`; local Minecraft server smoke collected `3` evidence events. |

## Summary Artifact

Artifact:

```text
driver-fabric/build/craftless-final-gameplay/artifacts/final-gameplay-summary.json
```

Content:

```json
{
  "status": "passed",
  "api": "http://127.0.0.1:18087",
  "clientId": "final-public-gameplay",
  "minecraftVersion": "1.21.6",
  "openapiActionCount": 20,
  "chatVerified": true,
  "stateObserved": true,
  "blockChangedOrItemPickedUp": true,
  "craftedAndEquipped": true,
  "entityInteracted": true,
  "usedTaskAction": false,
  "usedServerProvisioning": false,
  "selectedBlock": "world.block:356:63:532",
  "selectedRecipe": "recipe.handle:627",
  "equippedSlot": 8,
  "selectedEntity": "entity.handle-4"
}
```

## Public Generated API Path

The probe used the packaged `craftless` CLI distribution, connected to a real
local Minecraft `1.21.6` server, fetched the live per-client OpenAPI, and used
JSON-RPC `method=invoke` against generated action ids. It also captured
projection and stream artifacts:

- `client-openapi-connected.json`
- `client-actions.json`
- `client-resources.json`
- `client-rpc-openapi.json`
- `client-rpc-actions.json`
- `client-rpc-resources.json`
- `client-rpc-subscribe.json`
- `client-events-stream.sse`
- `public-agent-actions.jsonl`
- `public-agent-state.jsonl`
- `server-evidence.jsonl`

## Positive Proof

From `public-agent-actions.jsonl` and `public-agent-state.jsonl`:

- `player.chat` accepted message `Craftless CL-07 public gameplay probe`.
- `world.block.query { radius: 64, category: "log" }` accepted and returned
  runtime-discovered log targets with generic `material`, `collectable`, and
  `requires-tool` metadata.
- `navigation.follow` accepted and reached the selected material target.
- `world.block.break` accepted with `hit:true` and `changed:true`.
- `inventory.query` proved honest pickup: inventory changed from empty to
  `Jungle Log x1`.
- `recipe.query { craftable: true }` accepted and discovered a live
  `shapeless-crafting` recipe producing `Jungle Planks x4`.
- `recipe.craft` accepted with `changed:true`.
- `inventory.equip { slot: 8 }` accepted and `player.query` proved
  `selected-slot: 8`.
- `world.block.interact` accepted with the held `Jungle Planks`, and final
  inventory showed `Jungle Planks x3`, proving a world/item interaction used
  one plank.
- `entity.query { radius: 64 }` accepted and selected a same-level runtime
  entity target from public player/entity positions.
- `navigation.follow` accepted to the target entity.
- `entity.attack` accepted with `hit:true` against `entity.handle-4`.

Server evidence:

```json
{"type":"PLAYER_JOINED","player":"FinalPublic"}
{"type":"CHAT","player":"FinalPublic","message":"Craftless CL-07 public gameplay probe"}
{"type":"PLAYER_DISCONNECTED","player":"FinalPublic"}
```

## Negative Proof

The final probe ran with:

```text
CRAFTLESS_DISABLE_SMOKE_PROVISIONING=1
```

The summary records:

```json
{
  "usedTaskAction": false,
  "usedServerProvisioning": false
}
```

The script and Bun guard reject or avoid:

- static `task.*` actions;
- `task.survival`;
- `kill.cow`;
- `find.tree`;
- `craft.sword`;
- `/give`;
- direct `:driver-fabric:runClient` use;
- server-provisioned inventory.

## Generic Primitive Fixes Proven By This Run

- Block query radius now allows `64`, so spawn-robust useful material discovery
  can find runtime logs without a static resource recipe.
- Block query projection exposes generic material metadata:
  `collectable`, `material`, and `requires-tool`.
- The public probe uses runtime recipe discovery and selects a crafting recipe
  from `recipe.query`, instead of naming planks or a fixed recipe.
- Entity proof now uses public `player.query` plus `entity.query` positions to
  prefer same-level living targets before navigation and attack. It does not
  name a mob type.

## Remaining Gates

CL-08 remains open: rerun the final focused/local gates after these edits, then
commit, push `main`, and verify the worktree is clean.
