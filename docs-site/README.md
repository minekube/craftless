# Craftless Docs Site

This Fumadocs site builds a static OpenAPI reference for Cloudflare Workers
Static Assets.

Verify the docs site from the repository root:

```sh
mise run docs-site-verify
```

That regenerates the supervisor OpenAPI snapshot, installs with Bun's frozen
lockfile, typechecks the Fumadocs app, builds the static export, and runs a
Wrangler deploy dry-run.

Build only:

```sh
mise run docs-site-build
```

Production is served from Cloudflare Workers at:

```txt
https://craftless.minekube.com
```

Deploy manually from `docs-site/` when authenticated with Cloudflare:

```sh
mise exec -- bun run deploy
```

The `docs-site` GitHub Actions workflow builds and verifies on pushes to
`main`. It deploys to Cloudflare Workers only when the repository secret
`CLOUDFLARE_API_TOKEN` is present. That token must have Workers Scripts Write
and Account Settings Read scoped to the Cloudflare account, plus Workers Routes
Write and Zone Read scoped to the `minekube.com` zone. Wrangler attaches the
Worker to `craftless.minekube.com` through the custom domain route in
`wrangler.jsonc`. Preview URLs remain enabled on the Worker for ad-hoc
Wrangler version uploads.

If the repository secret ever needs to be recreated, create the Cloudflare
token in the Cloudflare dashboard, then set it on the GitHub repository:

```sh
gh secret set CLOUDFLARE_API_TOKEN --repo minekube/craftless --body "$TOKEN"
```
