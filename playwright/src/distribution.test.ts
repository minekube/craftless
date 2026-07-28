import { describe, expect, test } from "bun:test";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dir, "..", "..");

function exists(path: string): boolean {
  return existsSync(resolve(root, path));
}

function read(path: string): string {
  return readFileSync(resolve(root, path), "utf8");
}

describe("distribution surface", () => {
  test("release workflow, setup action, installer, and Docker runtime files exist", () => {
    expect(exists(".github/workflows/release.yml")).toBe(true);
    expect(exists(".github/actions/setup-craftless/action.yml")).toBe(true);
    expect(exists("install.sh")).toBe(true);
    expect(exists("Dockerfile")).toBe(true);
    expect(exists("docker/entrypoint.sh")).toBe(true);
  });

  test("CLI distribution packages driver mod manifest", () => {
    const cliBuild = read("cli/build.gradle.kts");
    const fabricBuild = read("driver-fabric/build.gradle.kts");
    const mise = read(".mise.toml");

    expect(cliBuild).toContain("driver-mods.json");
    expect(cliBuild).toContain("fabric-driver-lanes.json");
    expect(cliBuild).toContain("writeFabricDriverLaneCatalog");
    expect(cliBuild).toContain("renderDriverModManifest");
    expect(cliBuild).toContain("JsonSlurper");
    expect(cliBuild).toContain("stageFabricDriverLaneArtifacts");
    expect(cliBuild).toContain("runtimeMods");
    expect(cliBuild).toContain("fabric-current-remap-jar");
    expect(cliBuild).toContain("driver-lane-artifacts");
    expect(cliBuild).toContain("distributionPath");
    expect(fabricBuild).toContain("mods/craftless-driver-fabric.jar");
    expect(cliBuild).not.toContain("catalog.readText().trimEnd()");
    expect(cliBuild).not.toContain('extensions.extraProperties["fabricCompiledMinecraftVersion"]');
    expect(cliBuild).not.toContain('extensions.extraProperties["fabricCompiledLoaderVersion"]');
    expect(cliBuild).not.toContain('into("mods")');
    expect(mise).toContain("driver-mods.json");
    expect(mise).toContain("tar -tf cli/build/distributions/craftless-*.tar | grep -q '/driver-mods.json$'");
    expect(mise).toContain("build/driver-mods-from-tar.json");
    expect(mise).toContain("! grep -q 'artifactKey' build/driver-mods-from-tar.json");
    expect(mise).toContain("! grep -q 'distributionPath' build/driver-mods-from-tar.json");
    expect(mise).toContain("jar tf cli/build/distributions/craftless-*.zip | grep -q '/driver-mods.json$'");
    expect(mise).toContain("! unzip -p cli/build/distributions/craftless-*.zip '*/driver-mods.json' | grep -q 'artifactKey'");
    expect(mise).toContain("! unzip -p cli/build/distributions/craftless-*.zip '*/driver-mods.json' | grep -q 'distributionPath'");
  });

  test("CLI distribution packages representative older fabric lane", () => {
    const cliBuild = read("cli/build.gradle.kts");
    const fabricBuild = read("driver-fabric/build.gradle.kts");
    const mise = read(".mise.toml");

    expect(fabricBuild).toContain("craftless.fabric.distributionPath");
    expect(cliBuild).toContain("craftless.extraFabricDriverLaneRoot");
    expect(cliBuild).toContain("extraFabricDriverLaneRoot");
    expect(mise).toContain("-Pcraftless.fabric.artifactKey=fabric-1-20-6-remap-jar");
    expect(mise).toContain("-Pcraftless.fabric.distributionPath=mods/fabric-1.20.6/craftless-driver-fabric.jar");
    expect(mise).toContain("-Pcraftless.extraFabricDriverLaneRoot=build/driver-lanes");
    expect(mise).toContain("mods/fabric-1.20.6/craftless-driver-fabric.jar");
    expect(mise).not.toContain("mods/fabric-1.20.6/runtime/");
    expect(mise).toContain("mods/fabric-1.21.6/runtime/");
    expect(mise).toContain(":driver-fabric:preparePathfinderRuntime");
    expect(mise).toContain("-Pcraftless.fabric.runtimeMods=");
    expect(mise).toContain("baritone-api-fabric-");
    expect(mise).toContain("nether-pathfinder-");
    expect(mise).toContain("tar -tf cli/build/distributions/craftless-*.tar | grep -q '/mods/fabric-1.21.6/runtime/baritone-api-fabric-");
    expect(mise).toContain("grep -q 'runtimeMods' build/driver-mods-from-tar.json");
    expect(mise).toContain("jar tf cli/build/distributions/craftless-*.zip | grep -q '/mods/fabric-1.21.6/runtime/baritone-api-fabric-");
    expect(mise).toContain("unzip -p cli/build/distributions/craftless-*.zip '*/driver-mods.json' | grep -q 'runtimeMods'");
    expect(mise).toContain("minecraftVersion");
    expect(mise).toContain("1.20.6");
  });

  test("CLI distribution packages latest official fabric lane", () => {
    const mise = read(".mise.toml");

    expect(mise).toContain(":driver-fabric-official:jar");
    expect(mise).toContain("build/driver-lanes/latest-official");
    expect(mise).toContain("mods/fabric-26.2/craftless-driver-fabric-official.jar");
    expect(mise).toContain('\\"minecraftVersion\\": \\"26.2\\"');
    expect(mise).toContain('\\"fabricApiVersion\\": \\"0.153.0+26.2\\"');
    expect(mise).toContain('\\"javaMajorVersion\\": 25');
    expect(mise).toContain("java@temurin-25.0.3+9.0.LTS");
  });

  test("packaged latest current probe is a mise-managed product surface", () => {
    const mise = read(".mise.toml");
    const script = read("scripts/packaged-latest-current-probe.sh");

    expect(mise).toContain("[tasks.packaged-latest-current-probe]");
    expect(mise).toContain("CRAFTLESS_LOCAL_SERVER_SMOKE=1");
    expect(mise).toContain("CRAFTLESS_SMOKE_JAVA_EXECUTABLE=$HOME/.local/share/mise/installs/java/temurin-25.0.3+9.0.LTS/bin/java");
    expect(mise).toContain("CRAFTLESS_PACKAGED_LATEST_TIMEOUT_MS=900000");
    expect(mise).toContain("$PWD/scripts/packaged-latest-current-probe.sh");
    expect(mise).toContain("mise run package-cli");
    expect(script).toContain("build/docker/craftless/bin/craftless");
    expect(script).toContain("-F version=latest-release");
    expect(script).toContain("supervisor-openapi.json");
    expect(script).toContain("clients-create-latest-release.log");
    expect(script).toContain("client-openapi-connected.json");
    expect(script).toContain("client-rpc-subscribe.json");
    expect(script).toContain("client-generated-action-selected.json");
    expect(script).toContain("client-rpc-invoke-generated.json");
    expect(script).toContain("client-cli-invoke-generated.log");
    expect(script).toContain("CLIENT_INSTANCE_MARKER=");
    expect(script).toContain("client-stop-processes.log");
    expect(script).toContain("wait_for_client_exit");
    expect(script).toContain('$0 !~ /awk -v marker/');
    expect(script).toContain("x-craftless-actions");
    expect(script).toContain('!action.id.startsWith("task.")');
    expect(script).toContain('method: "invoke"');
    expect(script).toContain('api "/clients/$CLIENT_ID:run"');
    expect(script).toContain('-F "action=$GENERATED_ACTION_ID"');
    expect(script).toContain("mise exec -- bun");
    expect(script).not.toContain("task.survival");
  });

  test("packaged representative older probe is a matching product surface", () => {
    const mise = read(".mise.toml");
    const script = read("scripts/packaged-representative-older-probe.sh");

    expect(mise).toContain("[tasks.packaged-representative-older-probe]");
    expect(mise).toContain("CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.20.6");
    expect(mise).toContain("$PWD/scripts/packaged-representative-older-probe.sh");
    expect(mise).toContain("mise run package-cli");
    expect(script).toContain("build/docker/craftless/bin/craftless");
    expect(script).toContain("-F version=1.20.6");
    expect(script).toContain("-F loaderVersion=0.19.3");
    expect(script).toContain("clients-create-representative-older.log");
    expect(script).toContain("client-openapi-connected.json");
    expect(script).toContain("client-rpc-subscribe.json");
    expect(script).toContain("client-generated-action-selected.json");
    expect(script).toContain("client-rpc-invoke-generated.json");
    expect(script).toContain("client-cli-invoke-generated.log");
    expect(script).toContain("x-craftless-actions");
    expect(script).toContain('!action.id.startsWith("task.")');
    expect(script).toContain('method: "invoke"');
    expect(script).toContain('api "/clients/$CLIENT_ID:run"');
    expect(script).toContain('-F "action=$GENERATED_ACTION_ID"');
    expect(script).toContain("mise exec -- bun");
    expect(script).not.toContain("task.survival");
    expect(script).not.toContain(":driver-fabric:runClient");
  });

  test("packaged supported Fabric matrix probes all supported rows", () => {
    const mise = read(".mise.toml");
    const script = read("scripts/packaged-fabric-lane-probe.sh");
    const matrixScript = read("scripts/packaged-fabric-supported-matrix-probe.sh");
    const workflow = read(".github/workflows/fabric-support-matrix.yml");

    expect(mise).toContain("[tasks.packaged-current-lane-probe]");
    expect(mise).toContain("CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=$PWD/build/craftless-packaged-latest-current-probe");
    expect(mise).toContain("CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=$PWD/build/craftless-packaged-representative-older-probe");
    expect(mise).toContain("CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=$PWD/build/craftless-packaged-current-lane-probe");
    expect(mise).toContain("CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.21.6");
    expect(mise).toContain("CRAFTLESS_PACKAGED_FABRIC_VERSION=1.21.6");
    expect(mise).toContain("CRAFTLESS_PACKAGED_FABRIC_LOADER_VERSION=0.19.3");
    expect(mise).toContain("CRAFTLESS_PACKAGED_FABRIC_LABEL=current-lane");
    expect(mise).toContain("$PWD/scripts/packaged-fabric-lane-probe.sh");
    expect(mise).toContain("[tasks.packaged-fabric-supported-matrix-probe]");
    expect(mise).toContain("bash scripts/packaged-fabric-supported-matrix-probe.sh");
    expect(mise).toContain("CRAFTLESS_PACKAGED_MATRIX_JAVA_25_EXECUTABLE");
    expect(script).toContain("CRAFTLESS_PACKAGED_FABRIC_VERSION is required");
    expect(script).toContain("CRAFTLESS_PACKAGED_FABRIC_LOADER_VERSION");
    expect(script).toContain("supervisor-openapi.json");
    expect(script).toContain('PROFILE_NAME="Cf${PROFILE_SUFFIX:0:14}"');
    expect(script).toContain("client-openapi-connected.json");
    expect(script).toContain("client-rpc-invoke-generated.json");
    expect(script).toContain("client-cli-invoke-generated.log");
    expect(script).toContain("x-craftless-actions");
    expect(script).toContain('!action.id.startsWith("task.")');
    expect(script).toContain('method: "invoke"');
    expect(script).toContain('api "/clients/$CLIENT_ID:run"');
    expect(script).not.toContain(":driver-fabric:runClient");
    expect(script).not.toContain("task.survival");
    expect(matrixScript).toContain("/versions/runtime-targets");
    expect(matrixScript).toContain("/versions/support-targets");
    expect(matrixScript).toContain("target.runtimeTargets");
    expect(matrixScript).toContain("CRAFTLESS_PACKAGED_MATRIX_DISCOVERY_ONLY");
    expect(matrixScript).toContain("driver-mods.json");
    expect(matrixScript).toContain("support-target-validation-error.json");
    expect(matrixScript).toContain("probe-jobs.json");
    expect(matrixScript).toContain("probe-jobs.tsv");
    expect(matrixScript).toContain("CRAFTLESS_PACKAGED_FABRIC_VERSION=$REQUEST_VERSION");
    expect(matrixScript).toContain("CRAFTLESS_PACKAGED_FABRIC_LOADER_VERSION=$LOADER_VERSION");
    expect(matrixScript).toContain("CRAFTLESS_SMOKE_ACTION_COMMAND_JSON=[\\\"$ROOT/scripts/packaged-fabric-lane-probe.sh\\\"]");
    expect(matrixScript).toContain(":driver-fabric:fabricClientSmoke");
    expect(workflow).toContain("workflow_dispatch:");
    expect(workflow).toContain("schedule:");
    expect(workflow).toContain("Install Minecraft runtime packages");
    expect(workflow).toContain("xvfb");
    expect(workflow).toContain("mise run packaged-fabric-supported-matrix-probe");
    expect(workflow).toContain("if-no-files-found: warn");
    expect(workflow).toContain("build/craftless-packaged-fabric-supported-matrix");
    expect(workflow).toContain("build/craftless-packaged-current-lane-probe");
    expect(workflow).toContain("driver-fabric/build/craftless-packaged-current-lane-probe");
  });

  test("final public gameplay probe uses generated public surfaces only", () => {
    const mise = read(".mise.toml");
    const script = read("scripts/final-public-gameplay-probe.sh");

    expect(mise).toContain("[tasks.final-public-gameplay-probe]");
    expect(mise).toContain("CRAFTLESS_DISABLE_SMOKE_PROVISIONING=1");
    expect(mise).toContain("CRAFTLESS_SMOKE_MINECRAFT_VERSION=latest-release");
    expect(mise).toContain("$PWD/scripts/final-public-gameplay-probe.sh");
    expect(mise).not.toContain("CRAFTLESS_SMOKE_PROVISION_ITEM_ID=");
    expect(script).toContain("GET /clients/{id}/openapi.json authority");
    expect(script).toContain("missing-generic-primitive:");
    expect(script).toContain("player.chat");
    expect(script).toContain("inventory.query");
    expect(script).toContain("world.block.break");
    expect(script).toContain("navigation.plan");
    expect(script).toContain("navigation.follow");
    expect(script).toContain('radius: 64');
    expect(script).toContain("materialDropPosition");
    expect(script).toContain('category: "collectable"');
    expect(script).toContain("recipe-query-after-material");
    expect(script).toContain("recipe.craft");
    expect(script).toContain("craftingRecipe");
    expect(script).toContain("attempt === 1 ? 64 : 16");
    expect(script).toContain("entity-query-target-attempt");
    expect(script).toContain("entity-search-player");
    expect(script).toContain("verticalDelta");
    expect(script).toContain("entityNavigationBlocker");
    expect(script).toContain("entity.attack");
    expect(script).not.toContain("setTimeout(resolve, 1500));\nconst afterBreakInventory");
    expect(script).not.toContain("task.survival");
    expect(script).not.toContain("kill.cow");
    expect(script).not.toContain("find.tree");
    expect(script).not.toContain("craft.sword");
    expect(script).not.toContain("/give");
  });

  test("public docs make client creation lifecycle explicit", () => {
    const readme = read("README.md");
    const skill = read(".agents/skills/craftless-public-gameplay-agent/SKILL.md");

    for (const surface of [readme, skill].map((text) => text.replace(/\s+/g, " "))) {
      expect(surface).toContain("launches a new daemon-managed real Minecraft Java client process");
      expect(surface).toContain("not a selector, retry, or reuse operation");
      expect(surface).toContain("Creating fresh timestamped ids for retries leaves multiple Minecraft clients running");
      expect(surface).toContain("craftless api /clients/<id>:stop --api \"$CRAFTLESS\" -X POST");
    }
  });

  test("Dockerfile copies a built CLI distribution instead of building Craftless", () => {
    const dockerfile = read("Dockerfile");

    expect(dockerfile).toContain("COPY build/docker/craftless/");
    for (const forbidden of ["gradle", "mise", "npm", "yarn", "pnpm", "bun"]) {
      expect(dockerfile.toLowerCase()).not.toContain(forbidden);
    }
  });

  test("Docker runtime leaves lane selection to the packaged driver manifest", () => {
    const dockerfile = read("Dockerfile");

    for (const line of dockerfile.split("\n")) {
      expect(line.trim().startsWith("ENV CRAFTLESS_FABRIC_DRIVER_MOD")).toBe(false);
    }
  });

  test("CI runs the shipped Docker image through a newest-lane join, not a container start", () => {
    const mise = read(".mise.toml");
    const script = read("scripts/docker-image-latest-lane-probe.sh");
    const workflow = read(".github/workflows/docker-image-lane-check.yml");

    expect(mise).toContain("[tasks.docker-image-latest-lane-probe]");
    expect(mise).toContain("bash scripts/docker-image-latest-lane-probe.sh");
    expect(workflow).toContain("mise run docker-image-latest-lane-probe");
    expect(workflow).toContain("pull_request");
    expect(workflow).toContain("schedule");
    expect(workflow).toContain("Dockerfile");
    // The image itself is the artifact under test, and the join is the proof.
    expect(script).toContain('docker build "${PLATFORM_ARG[@]}" -t "$IMAGE" "$ROOT"');
    expect(script).toContain('test -z "${CRAFTLESS_FABRIC_DRIVER_MOD:-}"');
    expect(script).toContain("$PLAYER_NAME joined the game");
    expect(script).toContain("client.attached");
    expect(script).toContain("client-openapi-connected.json");
    expect(script).toContain("never reached play state");
    expect(script).toContain("teardown-verification.txt");
  });

  test("release workflow builds artifacts before Docker and publishes GitHub release assets", () => {
    const workflow = read(".github/workflows/release.yml");

    expect(workflow).toContain("mise run ci");
    expect(workflow).toContain("mise run package-cli");
    expect(workflow).toContain("softprops/action-gh-release");
    expect(workflow).toContain("generate_release_notes: true");
    expect(workflow).toContain("ubuntu-24.04-arm");
    expect(workflow).toContain("docker/build-push-action");
    expect(workflow).toContain("platform: linux/amd64");
    expect(workflow).toContain("platform: linux/arm64");
    expect(workflow).toContain("docker buildx imagetools create");
    expect(workflow).not.toContain("docker/setup-qemu-action");
    expect(workflow).not.toContain("platforms: linux/amd64,linux/arm64");
    expect(workflow).toContain("ghcr.io/minekube/craftless");
  });

  test("release workflow only moves the latest image tag forward", () => {
    const workflow = read(".github/workflows/release.yml");

    // `:latest` is a live pointer. Re-running this workflow at an older tag
    // must not drag it backwards, so the retag is gated on GitHub's own
    // newest-release answer rather than on a tag-name sort.
    expect(workflow).toContain("gh release list");
    expect(workflow).toContain("isLatest");
    expect(workflow).toContain("is_newest");
    expect(workflow).toContain('if [ "$IS_NEWEST" = "true" ]; then');
    expect(workflow).toContain('TAGS+=(--tag "${REGISTRY_IMAGE}:latest")');
    expect(workflow).not.toContain('--tag "${REGISTRY_IMAGE}:latest" \\');
  });

  test("release repair workflow rebuilds tagged source without registry credentials", () => {
    const workflow = read(".github/workflows/release-repair.yml");

    expect(exists(".github/workflows/release-repair.yml")).toBe(true);
    expect(workflow).toContain("release_tag");
    expect(workflow).toContain("required: true");
    expect(workflow).toContain("ref: ${{ inputs.release_tag }}");
    expect(workflow).toContain("mise run ci");
    expect(workflow).toContain("mise run package-cli");
    expect(workflow).toContain("gh release upload");

    // The safety of the repair path is a capability boundary, not a
    // condition: with no registry credential and no packages: write, moving
    // ghcr.io/minekube/craftless:latest backwards is unrepresentable here.
    // If any of these ever appear, the whole property is gone.
    expect(workflow).toContain("contents: write");
    expect(workflow).not.toContain("packages: write");
    expect(workflow).not.toContain("docker/login-action");
    expect(workflow).not.toContain("imagetools");
    expect(workflow).not.toContain("REGISTRY_IMAGE");

    // The repair path must never gain a general test-bypass. A mechanism that
    // can skip a tag's own tests is a durable supply-chain capability that
    // will be reached for again, by someone with less context, on a tag whose
    // failing test does test software. `mise run ci` runs unconditionally and
    // there is no input that can turn it off.
    expect(workflow).not.toMatch(/skip[-_](test|tests|ci|verify|checks)/i);
    expect(workflow).not.toMatch(/build[-_]only/i);
    const inputNames = [...workflow.matchAll(/^ {6}(\w+):$/gm)].map((m) => m[1]);
    expect(inputNames).toEqual(["release_tag"]);

    // Repair fills holes and proves the landed result; it never edits release
    // metadata and never trusts its own upload step.
    expect(workflow).not.toContain("softprops/action-gh-release");
    expect(workflow).toContain("Refuse to repair a complete release");
    expect(workflow).toContain("Verify published release assets");
  });

  test("release troubleshooting doc records the permanently unrepairable tags", () => {
    const doc = read("docs/release-troubleshooting.md");

    for (const tag of ["v0.2.0", "v0.3.0", "v0.3.1"]) {
      expect(doc).toContain(tag);
    }
    // The reason is the durable artifact, not the tag list: each tag's own
    // test suite asserts the manifest still holds the previous version, so
    // its gate is red at itself forever.
    expect(doc).toContain("playwright/src/distribution.test.ts");
    expect(doc).toContain('expect(manifest["."]).toBe("0.3.0")');
    expect(doc).toContain("What this does not fix");

    // These releases are empty because someone shipped a guard that cannot
    // pass - a check that exercises no product code - NOT because the software
    // is bad or a build broke. Wording that implies otherwise misrepresents
    // shipped releases to users, so the honest sentence is pinned here.
    // Prose wraps across lines, so compare on normalised whitespace.
    const prose = doc.replace(/\s+/g, " ");

    expect(prose).toContain(
      "the software is fine; the release carries no downloadable build because a " +
        "self-invalidating metadata guard in that tag prevents a clean rebuild",
    );
    expect(prose).toContain("Do not describe them as failing quality checks");

    // The repair path must never gain a general test-bypass; the only shape
    // ever allowed is one named metadata test excluded with evidence.
    expect(prose).toContain("The repair path never gains a general test-bypass");
    expect(prose).toContain("named exclusion with evidence");

    expect(doc).not.toContain("### Moving the container image after a repair");
    expect(doc).not.toContain("Only then dispatch `release.yml` on that tag");
    expect(prose).toContain("release-repair.yml` publishes ASSETS ONLY");
    expect(prose).toContain(
      "cannot be performed by dispatching that tag's own `release.yml`",
    );
    expect(prose).toContain(
      "v0.3.2`'s `release.yml` tags `:latest` inside the build-push step (line 78)",
    );
    expect(prose).toContain(
      "v0.3.4` and `v0.3.5` carry an unconditional `--tag \"${REGISTRY_IMAGE}:latest\"` in the imagetools step (line 125)",
    );
    expect(prose).toContain(
      "separately authorized, credential-holding workflow on the default branch",
    );
    expect(prose).toContain("deliberately not built here");
  });

  test("scheduled Release Please workflow creates release tags from main changes", () => {
    const workflow = read(".github/workflows/release-please.yml");
    const config = JSON.parse(read("release-please-config.json"));
    const manifest = JSON.parse(read(".release-please-manifest.json"));
    const changelog = read("CHANGELOG.md");

    expect(workflow).toContain('branches: ["main"]');
    expect(workflow).toContain("schedule:");
    expect(workflow).toContain('cron: "17 8 * * 1"');
    expect(workflow).toContain("workflow_dispatch:");
    expect(workflow).toContain("googleapis/release-please-action@v4");
    expect(workflow).toContain("config-file: release-please-config.json");
    expect(workflow).toContain("manifest-file: .release-please-manifest.json");
    expect(workflow).toContain("pull-requests: write");

    // A GITHUB_TOKEN tag push does not fire release.yml's `push: tags:`
    // trigger, so without this dispatch the tag exists and the release
    // publishes empty. This is the bridge, not a convenience.
    expect(workflow).toContain("actions: write");
    expect(workflow).toContain("trigger-release:");
    expect(workflow).toContain("release_created == 'true'");
    expect(workflow).toContain("gh workflow run release.yml");
    expect(workflow).toContain('--ref "$TAG_NAME"');

    expect(config.packages["."]["package-name"]).toBe("craftless");
    expect(config.packages["."]["release-type"]).toBe("simple");
    expect(config.packages["."]["include-v-in-tag"]).toBe(true);
    expect(config.packages["."]["include-component-in-tag"]).toBe(false);
    expect(manifest["."]).toMatch(/^\d+\.\d+\.\d+$/);
    expect(changelog).toContain("Release Please");
  });

  test("active completion checklist names the current published release", () => {
    const manifest = JSON.parse(read(".release-please-manifest.json"));
    const checklist = read("docs/project-completion-checklist.md");
    const currentReleaseTag = `v${manifest["."]}`;

    expect(checklist).toContain(currentReleaseTag);
    expect(checklist).not.toContain("Release `v0.3.2` is published.");
    expect(checklist).not.toContain("Latest published release before Phase 209: `v0.3.2`");
  });

  test("Release Please bumps the checklist rows this guard asserts on", () => {
    const config = JSON.parse(read("release-please-config.json"));
    const checklist = read("docs/project-completion-checklist.md");

    // Without this wiring the guard above is self-invalidating: the release
    // commit bumps .release-please-manifest.json and nothing else, so the
    // checklist names the PREVIOUS version and `mise run ci` is red at the
    // tag it was cut from - which then fails release.yml's Verify step and
    // publishes an empty release. That is exactly how v0.3.5 shipped with no
    // assets, and the same shape that makes v0.2.0/v0.3.0/v0.3.1 permanently
    // unrepairable. See docs/release-troubleshooting.md.
    const extraFiles = config.packages["."]["extra-files"] ?? [];
    expect(
      extraFiles.some(
        (entry: { type?: string; path?: string }) =>
          entry.type === "generic" && entry.path === "docs/project-completion-checklist.md",
      ),
    ).toBe(true);

    // Every checklist line naming the release tag must carry the annotation
    // the generic updater keys on, or Release Please silently skips it.
    const taggedLines = checklist
      .split("\n")
      .filter((line) => /Release `v\d+\.\d+\.\d+`|releases\/tag\/v\d+\.\d+\.\d+/.test(line));

    expect(taggedLines.length).toBeGreaterThan(0);
    for (const line of taggedLines) {
      expect(line).toContain("x-release-please-version");
    }

    // The generic updater applies `line.replace(VERSION_REGEX, ...)` with a
    // NON-global regex, so it rewrites only the FIRST version on a line. A row
    // mentioning the tag twice (prose plus a .../releases/tag/vX.Y.Z URL) would
    // half-update and go quietly stale. One version per annotated line.
    for (const line of checklist.split("\n")) {
      if (!line.includes("x-release-please-version")) continue;
      expect(line.match(/\d+\.\d+\.\d+/g) ?? []).toHaveLength(1);
    }
  });

  test("Fumadocs site is a Cloudflare Workers product surface with previews", () => {
    const mise = read(".mise.toml");
    const packageJson = JSON.parse(read("docs-site/package.json"));
    const wrangler = read("docs-site/wrangler.jsonc");
    const nextConfig = read("docs-site/next.config.mjs");
    const openapi = read("docs-site/lib/openapi.ts");
    const source = read("docs-site/lib/source.ts");
    const apiPage = read("docs-site/components/api-page.tsx");
    const page = read("docs-site/app/docs/[[...slug]]/page.tsx");
    const apiReference = read("docs-site/content/docs/api-reference.mdx");
    const cliDocs = read("docs-site/content/docs/cli.mdx");
    const meta = JSON.parse(read("docs-site/content/docs/meta.json"));
    const schema = JSON.parse(read("docs-site/openapi/craftless-supervisor.json"));

    expect(packageJson.scripts.build).toBe("next build");
    expect(packageJson.scripts.deploy).toBe("wrangler deploy");
    expect(packageJson.scripts["preview:upload"]).toContain("wrangler versions upload");
    expect(packageJson.scripts["openapi:generate"]).toContain("mise run docs-site-openapi");
    expect(packageJson.dependencies["fumadocs-openapi"]).toBeDefined();
    expect(packageJson.dependencies["fumadocs-ui"]).toBeDefined();
    expect(packageJson.dependencies.next).toBeDefined();
    expect(packageJson.devDependencies.wrangler).toBeDefined();
    expect(nextConfig).toContain("output: 'export'");
    expect(nextConfig).toContain("trailingSlash: true");
    expect(openapi).toContain("createOpenAPI");
    expect(openapi).toContain("./openapi/craftless-supervisor.json");
    expect(source).toContain("openapi.staticSource");
    expect(source).toContain("openapi.loaderPlugin()");
    expect(apiPage).toContain("createOpenAPIPage");
    expect(page).toContain("getOpenAPIPageProps()");
    expect(schema.openapi).toBe("3.1.0");
    expect(JSON.stringify(schema)).not.toContain("x-craftless-cli");
    expect(schema.paths["/openapi.json"].get.description).toContain("stable supervisor API");
    expect(schema.tags.find((tag: { name: string }) => tag.name === "clients")?.description).toContain(
      "Daemon-managed real Minecraft Java clients",
    );
    expect(apiReference).toContain("Generated operation pages are grouped by Craftless API pillar");
    expect(meta.pages).toContain("cli");
    expect(cliDocs).toContain("craftless daemon start");
    expect(cliDocs).toContain("craftless api <endpoint>");
    expect(cliDocs).toContain("OpenAPI-derived route help");
    expect(cliDocs).toContain("-f jsonrpc=2.0");
    expect(cliDocs).not.toContain("craftless clients create");
    expect(mise).toContain("[tasks.docs-site-openapi]");
    expect(mise).toContain("[tasks.docs-site-build]");
    expect(mise).toContain("mise exec -- bun install");
    expect(mise).toContain("mise exec -- bun run build");
    expect(wrangler).toContain('"name": "craftless-docs"');
    expect(wrangler).toContain('"preview_urls": true');
    expect(wrangler).toContain('"directory": "./out"');
    expect(wrangler).toContain('"not_found_handling": "404-page"');
    expect(exists(".github/workflows/docs-pages.yml")).toBe(false);
  });

  test("install script installs from minekube/craftless GitHub releases", () => {
    const install = read("install.sh");

    expect(install).toContain("minekube/craftless");
    expect(install).toContain("api.github.com/repos/${CRAFTLESS_REPOSITORY}/releases/latest");
    expect(install).toContain("releases/download");
    expect(install).toContain("craftless-${asset_version}.tar");
    expect(install).toContain("CRAFTLESS_INSTALL_DIR");
  });

  test("setup action installs Craftless and can start the daemon", () => {
    const action = read(".github/actions/setup-craftless/action.yml");

    expect(action).toContain("description:");
    expect(action).toContain("version:");
    expect(action).toContain("start:");
    expect(action).toContain("api-url");
    expect(action).toContain("craftless daemon start");
  });

  test("README exposes install, Docker, and GitHub Actions quickstarts", () => {
    const readme = read("README.md");
    const manifest = JSON.parse(read(".release-please-manifest.json"));
    const currentReleaseTag = `v${manifest["."]}`;

    expect(readme).toContain("## Quickstart");
    expect(readme).toContain("curl -fsSL https://raw.githubusercontent.com/minekube/craftless/main/install.sh");
    // The README install example must name the current release so future
    // releases cannot leave users pinned to an older, empty release.
    expect(readme).toContain(`CRAFTLESS_VERSION=${currentReleaseTag}`);
    expect(readme).toContain("docker run");
    expect(readme).toContain(`minekube/craftless/.github/actions/setup-craftless@${currentReleaseTag}`);
    expect(readme).toContain("Release Please opens or updates the release PR");
    expect(readme).not.toContain("setup-craftless@v0.1.0");
    expect(readme).toContain("Minecraft artifacts are downloaded into the workspace at runtime");
    expect(readme).toContain("Latest/current `26.2`, current `1.21.6`, and representative older `1.20.6`");
    expect(readme).not.toContain("gameplay actions still empty");
    expect(readme).not.toContain("final completion still requires a refreshed run after latest/current compatibility work");
    expect(readme.toLowerCase()).not.toContain("homebrew");
    expect(readme.toLowerCase()).not.toContain("brew install");
  });

  test("README does not present legacy diagnostic gameplay setup as product status", () => {
    const readme = read("README.md");

    expect(readme).not.toContain("--loader-version 0.17.2");
    expect(readme).not.toContain("provisions an `Iron Sword`");
    expect(readme).not.toContain("target-item provisioning");
    expect(readme).not.toContain("CRAFTLESS_SMOKE_PROVISION_ITEM");
    expect(readme).toContain("without server-provisioned inventory");
  });

  test("README presents generated API as product path and retired bridge path", () => {
    const readme = read("README.md");

    expect(readme).toContain("generated per-client OpenAPI");
    expect(readme).toContain("runtime capability graph");
    expect(readme).toContain("retired from active build and package");
    expect(readme).toContain("not in a legacy launch adapter");
    expect(readme).not.toContain("HeadlessMC command");
    expect(readme).not.toContain("HMC-Specifics command");
  });

  test("active docs prefer latest aliases over concrete latest ids", () => {
    const readme = read("README.md");
    const roadmap = read("docs/roadmap.md");
    const fileManagement = read("docs/client-file-management.md");

    expect(readme).toContain('"version": "latest-release"');
    expect(readme).toContain("/cache:prepare");
    expect(readme).toContain("minecraftVersion=latest-release");
    expect(fileManagement).toContain("latest-release");
    expect(fileManagement).toContain("latest-snapshot");
    expect(roadmap).not.toContain("current latest `26.2`");
    expect(roadmap).toContain("latest-release");
  });

  test("installer and release workflow do not require Homebrew", () => {
    const install = read("install.sh");
    const workflow = read(".github/workflows/release.yml");

    expect(install.toLowerCase()).not.toContain("brew");
    expect(workflow.toLowerCase()).not.toContain("brew");
  });
});
