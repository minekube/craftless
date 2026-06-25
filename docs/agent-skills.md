# Agent Skills

Craftwright should keep repo-local agent skills narrow and relevant to the
current Kotlin/JVM work. Broad Android, KMP, JPA, or one-off migration skills
should not be installed unless the repository starts using those technologies.

## Researched Sources

- JetBrains/Kotlin publishes `Kotlin/kotlin-agent-skills`, an Apache-2.0
  collection following the Agent Skills standard.
- Kotlin's documentation describes these skills as reusable instructions for
  Kotlin-specific agent workflows and lists Codex compatibility.

Current upstream skill folders:

- `kotlin-tooling-java-to-kotlin`
- `kotlin-tooling-immutable-collections-0-5-x-migration`
- `kotlin-backend-jpa-entity-mapping`
- `kotlin-tooling-agp9-migration`
- `kotlin-tooling-cocoapods-spm-migration`

## Recommendation For This Repository

Do not install the whole `Kotlin/kotlin-agent-skills` set into Craftwright right
now. Four of the five upstream skills are not relevant to this repo's current
Kotlin/JVM server, CLI, Fabric-driver, and TypeScript SDK work:

- no Android Gradle Plugin migration;
- no CocoaPods or SwiftPM migration;
- no JPA/Hibernate model;
- no `kotlinx.collections.immutable` dependency.

Install `kotlin-tooling-java-to-kotlin` only when there is a concrete conversion
task. Craftwright intentionally keeps Java for Fabric Mixins and bytecode-facing
Minecraft glue, so a Java-to-Kotlin skill should not be globally active by
default.

## Useful Install Commands

For a Codex plugin install:

```sh
codex plugin marketplace add Kotlin/kotlin-agent-skills
```

Then install `kotlin-agent-skills@Kotlin` from `/plugins`.

For manual repo-local installation of one upstream skill:

```sh
mkdir -p .agent/skills
cp -R path/to/kotlin-agent-skills/skills/kotlin-tooling-java-to-kotlin .agent/skills/
```

Do this only when that workflow is needed by a branch.
