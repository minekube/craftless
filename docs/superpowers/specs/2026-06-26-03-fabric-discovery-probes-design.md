# Fabric Discovery Probes Design

**Goal:** Fill the runtime capability graph from the live Fabric/Minecraft client without adding one public action at a time.

**Architecture:** `driver-fabric` owns internal probes that inspect the client thread, Fabric Loader, registries, mappings, callbacks, screens, handlers, player/world/entity/inventory state, and installed mods. Probes emit graph nodes plus private evidence; projection code decides the public Craftless shape.

**Probe Families:**
- Loader/mod probe: loader version, driver version, installed mods, environment.
- Registry probe: items, blocks, entity types, screen handlers, status effects, game events.
- Client-state probe: connected state, player, camera, world, interaction manager, current screen.
- Inventory/container probe: slots, selected slot, item stacks, container handles.
- World interaction probe: block target affordances, placement/break interaction availability, raycast target handles.
- Screen/handler probe: current screen, closeability, container/action affordances.
- Event source probe: Fabric callbacks, lightweight mixin/accessor event emitters, driver-generated lifecycle/action events.

**Rules:**
- Probes may use Fabric/Yarn/Minecraft internals privately.
- Probes must run Minecraft access on the client thread.
- Probes must produce unavailable metadata with machine-readable reasons when an affordance is detected but not executable.
- Probes must not directly create public OpenAPI descriptors.

**Completion Gate:**
- Tests prove probes populate graph nodes for current chat/movement/player/inventory/world/screen affordances.
- Tests fail when a new public gameplay descriptor bypasses graph projection.
- Real client smoke records graph evidence artifacts.
