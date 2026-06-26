#!/usr/bin/env sh
set -eu

CRAFTLESS_REPOSITORY="${CRAFTLESS_REPOSITORY:-minekube/craftless}"
CRAFTLESS_VERSION="${CRAFTLESS_VERSION:-latest}"
CRAFTLESS_INSTALL_DIR="${CRAFTLESS_INSTALL_DIR:-${HOME:-/tmp}/.local/bin}"
CRAFTLESS_HOME="${CRAFTLESS_HOME:-${HOME:-/tmp}/.craftless/cli}"

need() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "error: $1 is required" >&2
    exit 1
  fi
}

need curl
need tar

if [ "$CRAFTLESS_VERSION" = "latest" ]; then
  latest_json="$(curl -fsSL "https://api.github.com/repos/${CRAFTLESS_REPOSITORY}/releases/latest")"
  CRAFTLESS_VERSION="$(printf '%s' "$latest_json" | sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' | head -n 1)"
  if [ -z "$CRAFTLESS_VERSION" ]; then
    echo "error: could not resolve latest Craftless release" >&2
    exit 1
  fi
fi

asset_version="${CRAFTLESS_VERSION#v}"
asset_name="craftless-${asset_version}.tar"
download_url="https://github.com/${CRAFTLESS_REPOSITORY}/releases/download/${CRAFTLESS_VERSION}/${asset_name}"
tmp_dir="$(mktemp -d)"
install_root="${CRAFTLESS_HOME}/${asset_version}"

cleanup() {
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

curl -fsSL "$download_url" -o "$tmp_dir/$asset_name"
mkdir -p "$install_root"
tar -xf "$tmp_dir/$asset_name" -C "$install_root" --strip-components=1

mkdir -p "$CRAFTLESS_INSTALL_DIR"
ln -sfn "$install_root/bin/craftless" "$CRAFTLESS_INSTALL_DIR/craftless"

echo "craftless ${asset_version} installed to ${CRAFTLESS_INSTALL_DIR}/craftless"
case ":${PATH:-}:" in
  *":$CRAFTLESS_INSTALL_DIR:"*) ;;
  *) echo "add ${CRAFTLESS_INSTALL_DIR} to PATH if craftless is not found" ;;
esac
