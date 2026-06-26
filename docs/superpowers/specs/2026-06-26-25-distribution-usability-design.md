# Distribution Usability Design

## Intent

Make Craftless easy for other repositories and CI jobs to install and run
without cloning this repository or building from source. This phase pauses
additional gameplay primitives and focuses on the first supported distribution
surface.

## Product Rules

- Use `mise` only for this repository's build and release workflow.
- Consumers should not need Gradle or mise to use a released CLI.
- Do not add Homebrew in this phase.
- The Docker image is a runtime image. It copies an already-built Craftless CLI
  distribution into the image and must not compile the repository inside Docker.
- Do not bundle Minecraft client artifacts in the image. Craftless downloads
  and caches Minecraft/Fabric artifacts into the configured workspace at
  runtime through the API/CLI.
- Keep public package/domain names aligned with `minekube.com` and
  `com.minekube.craftless`.

## Release Surface

Tag releases with `v*`. A release workflow should:

1. run repository verification through `mise run ci`;
2. build the JVM CLI distribution archive;
3. generate checksums;
4. upload archives and checksums to GitHub Releases;
5. build and push a runtime Docker image to GitHub Container Registry.

The release workflow owns building artifacts. Docker only receives those
artifacts.

## Install Surface

Add an `install.sh` script for Linux/macOS that:

- resolves a version from `CRAFTLESS_VERSION` or `latest`;
- downloads the corresponding GitHub Release CLI archive;
- installs a symlink into `${CRAFTLESS_INSTALL_DIR:-$HOME/.local/bin}`;
- prints a concise success message.

The script should be simple and auditable. Checksums may be documented and
used by CI/release validation, but the first script does not need to implement
platform-specific package manager behavior.

## Docker Surface

Add a root `Dockerfile` that:

- starts from a Java 21 runtime base image;
- installs only runtime OS libraries needed for headless/visible Minecraft
  client automation;
- copies the built CLI distribution from a Docker build context path;
- exposes the Craftless API port;
- uses `/var/lib/craftless` as the default workspace.

## GitHub Action Surface

Add a composite action under `.github/actions/setup-craftless` that:

- downloads and installs the released CLI by version;
- optionally starts `craftless server start` in the caller's workflow;
- writes the API URL to outputs when the server starts.

This action should be reusable from external repositories after tags exist.

## Documentation

README should lead with:

- install script usage;
- Docker usage;
- GitHub Actions usage;
- release artifact expectations;
- clear note that Minecraft artifacts are downloaded into a workspace at
  runtime, not bundled in the image.

## Evidence

Automated checks must assert that:

- release workflow builds artifacts before Docker;
- Dockerfile does not run Gradle, mise, npm, yarn, pnpm, or Bun;
- Dockerfile copies a built CLI distribution;
- install script downloads from `minekube/craftless` GitHub Releases;
- setup action exposes install/start behavior;
- README contains install, Docker, and GitHub Actions quickstarts.
