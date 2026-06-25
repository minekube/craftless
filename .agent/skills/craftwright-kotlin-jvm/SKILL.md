---
name: craftwright-kotlin-jvm
description: Use when changing Craftwright Kotlin/JVM, Gradle, Ktor local API, protocol, CLI, bridge, Fabric driver, tests, or TypeScript integration code in this repository.
---

# Craftwright Kotlin/JVM

Craftwright is Kotlin/JVM-first. Keep changes aligned with the existing Gradle
multi-project shape and the local API/Fabric-driver roadmap.

## Repository Rules

- Run tools through `mise exec --`; use `mise exec -- gradle ...` for JVM work.
- Use Bun for TypeScript work: `mise exec -- bun ...`. Do not introduce npm or
  node package-manager commands.
- Keep generated/local HTTP API code on Ktor: Ktor Server/CIO for the daemon and
  Ktor Client/CIO for JVM clients and tests.
- Do not add OkHttp, `com.sun.net.httpserver.HttpServer`, Java `HttpClient`
  for normal implementation, or repo-owned HTTP method enums.
- Model route methods as protocol data strings such as `"GET"` and `"POST"`
  unless the route framework itself supplies a native type at the call site.
- Prefer `kotlinx.serialization` DTOs at module boundaries and keep OpenAPI
  contract data in the `protocol` module.
- Use coroutines for asynchronous API/server logic and close Ktor clients,
  servers, and coroutine scopes deterministically in tests.

## Code Shape

- Follow existing package/module ownership before adding abstractions.
- Keep Java only where Minecraft/Fabric/Mixin bytecode glue needs it; do not
  convert those files just to make the tree all-Kotlin.
- Keep public Craftwright API names independent from HeadlessMC/HMC-Specifics
  command strings; bridge details belong in bridge modules and docs.
- Prefer small immutable data classes, `val`, explicit null handling, and
  focused services over framework-heavy architecture.
- Test behavior at the module boundary that changed: protocol catalog/OpenAPI,
  daemon API routes, CLI command output, bridge behavior, or test fixtures.

## Verification

Before claiming completion, run the narrow relevant Gradle test first, then
`mise exec -- gradle test`. For TypeScript modules, also run the relevant
`mise exec -- bun ...` command.
