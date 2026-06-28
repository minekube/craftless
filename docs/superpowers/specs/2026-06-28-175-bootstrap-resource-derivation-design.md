# Bootstrap Resource Derivation Design

## Problem

Fabric bootstrap operation definitions still duplicated public resource
ownership with a `resource: String` field and repeated `resource = "..."`
catalog literals next to operation ids.

That duplication keeps the transitional bootstrap list shaped like a public
descriptor catalog. It also makes it easier to add a new gameplay operation by
copying a descriptor/binding pair instead of improving discovery and graph
projection.

## Design

Remove public resource ownership from
`FabricBootstrapOperationDefinition`. Runtime operation resources are derived
from operation ids using the same `substringBeforeLast(".")` convention used
by protocol and daemon projection code.

The Fabric client-state discovery fragment exposes the derived resource nodes
needed by bootstrap operations, including `world.block` and `world.time`.
Handles should point at the most specific derived resource they belong to.

## Non-Goals

- Do not add gameplay operations.
- Do not remove the entire bootstrap operation definition layer.
- Do not claim CL-02 is complete.
- Do not add static CLI commands or alias routes.
- Do not change the stable supervisor API.
- Do not make a new Minecraft version support claim.

## Acceptance

- Bootstrap operation definitions no longer contain a `resource: String`
  field.
- Bootstrap operation definitions no longer contain hand-maintained
  `resource = "player"` style public resource literals.
- Runtime operation resources are derived from operation ids.
- Client-state discovery emits resources required by derived bootstrap
  operation resources.
- Focused Fabric tests and discovery tests pass.
