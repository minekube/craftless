# HMC Bridge Module Instructions

`bridge-hmc/` is temporary evidence infrastructure for launching and controlling
real clients before the Fabric driver is complete.

## Scope

- HeadlessMC/HMC-Specifics launch and bridge experiments.
- Internal command mapping.
- Opt-in real-client smoke planning and evidence capture.

## Rules

- Never expose HeadlessMC or HMC-Specifics command strings as public API names,
  JSON fields, CLI verbs, SDK methods, or docs contracts.
- Label bridge behavior as bridge-only evidence. Do not describe it as robust
  movement, perception, inventory, or final automation.
- Keep real-client smoke tests opt-in and guarded by environment variables.
- Default tests must not download Minecraft/server artifacts or launch a real
  client.

## Verification

```sh
mise exec -- gradle :bridge-hmc:test
```
