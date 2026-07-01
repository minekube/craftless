# Headless Presentation Truth Design

## Goal

Craftless must not open a visible Minecraft window after an API caller requests
`presentation.window = NONE`. If the current host has no real windowless launch
strategy, Craftless should reject the client creation before starting Java.

## Research

HeadlessMC uses two legitimate no-window families: patch LWJGL so rendering
calls become stubs, or run Minecraft behind a virtual display such as Xvfb. Its
LWJGL path requires an agent/transformer, `-Djoml.nounsafe=true`, and Fabric
system-library wiring. Craftless should not expose HeadlessMC names publicly,
but the core lesson applies: a normal Minecraft launch is not headless.

## Design

For this slice, keep Craftless's existing virtual-display wrapper strategy.
`ClientWindowMode.NONE` requires a non-empty windowless command prefix, either
from automatic Linux `xvfb-run` discovery or `CRAFTLESS_WINDOWLESS_WRAPPER`.
When no prefix is available, the launcher throws an actionable error before
copying mods, writing muted options, creating logs, or starting the client
process.

`ClientWindowMode.VISIBLE` continues to bypass wrappers and launch normally.
Muted audio remains independent: it is materialized only after the window
strategy is valid.

## Follow-Up

A true cross-platform no-window renderer can be added later as a separate
Craftless-owned strategy, likely by adapting the HeadlessMC-style LWJGL
instrumentation pattern or by introducing an offscreen-rendering driver mode.
That follow-up should model capabilities explicitly instead of weakening
`window=NONE`.
