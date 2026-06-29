# HeadlessMC/HMC-Specifics Bridge Limitations

Craftless's Phase 1 bridge backend is temporary evidence infrastructure. It
may launch and connect a real Minecraft Java client through HeadlessMC and
HMC-Specifics, but public routes, CLI output, and gameplay contracts must
remain Craftless-owned.

The bridge must not expose HeadlessMC or HMC-Specifics command strings as public
API names, JSON fields, CLI verbs, or SDK methods. Those command strings are an
internal adapter detail.

The `driver-runtime` module can adapt this bridge behind `DriverSession`, but
that adapter is still only Phase 1 evidence. The durable backend remains the
Fabric driver running inside the client JVM.

Known limitations:

- Movement is simulated through bridge input and must not be described as robust
  player movement.
- First-run screens, title screens, focus, and current GUI state can swallow
  movement input.
- Usernames longer than 16 characters fail offline login packet encoding.
- Rendered text and server logs are useful evidence, but they are not the final
  structured event or perception API.
- Nearby blocks, nearby entities, raycasts, inventory, screen state, and clicks
  need a Craftless Fabric driver with direct Minecraft client API access.

The HMC bridge is now lifecycle evidence only. It does not publish or execute
Craftless gameplay actions such as chat, movement, look, raycast, inventory,
entity, block, or screen operations. Gameplay actions and resources must come
from the Fabric runtime capability graph and generated per-client OpenAPI. The
daemon exposes `/clients/{id}/openapi.json` with client metadata plus
graph-projected action/resource schemas, `GET /clients/{id}/actions` and
`GET /clients/{id}/resources` for discovery, `POST /clients/{id}:run` as the
stable generic invocation path, and generated routes derived from those live
action descriptors.
