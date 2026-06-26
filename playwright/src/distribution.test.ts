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

  test("Dockerfile copies a built CLI distribution instead of building Craftless", () => {
    const dockerfile = read("Dockerfile");

    expect(dockerfile).toContain("COPY build/docker/craftless/");
    for (const forbidden of ["gradle", "mise", "npm", "yarn", "pnpm", "bun"]) {
      expect(dockerfile.toLowerCase()).not.toContain(forbidden);
    }
  });

  test("release workflow builds artifacts before Docker and publishes GitHub release assets", () => {
    const workflow = read(".github/workflows/release.yml");

    expect(workflow).toContain("mise run ci");
    expect(workflow).toContain("mise run package-cli");
    expect(workflow).toContain("softprops/action-gh-release");
    expect(workflow).toContain("docker/build-push-action");
    expect(workflow).toContain("ghcr.io/minekube/craftless");
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
    expect(action).toContain("craftless server start");
  });

  test("README exposes install, Docker, and GitHub Actions quickstarts", () => {
    const readme = read("README.md");

    expect(readme).toContain("## Quickstart");
    expect(readme).toContain("curl -fsSL https://raw.githubusercontent.com/minekube/craftless/main/install.sh");
    expect(readme).toContain("docker run");
    expect(readme).toContain("minekube/craftless/.github/actions/setup-craftless");
    expect(readme).toContain("Minecraft artifacts are downloaded into the workspace at runtime");
    expect(readme.toLowerCase()).not.toContain("homebrew");
    expect(readme.toLowerCase()).not.toContain("brew install");
  });

  test("installer and release workflow do not require Homebrew", () => {
    const install = read("install.sh");
    const workflow = read(".github/workflows/release.yml");

    expect(install.toLowerCase()).not.toContain("brew");
    expect(workflow.toLowerCase()).not.toContain("brew");
  });
});
