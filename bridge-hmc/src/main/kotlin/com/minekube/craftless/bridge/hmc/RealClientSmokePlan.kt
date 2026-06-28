package com.minekube.craftless.bridge.hmc

data class RealClientSmokePlan(
    val environmentGate: String,
    val steps: List<SmokeStep>,
    val artifacts: List<String>,
) {
    companion object {
        fun default(): RealClientSmokePlan =
            RealClientSmokePlan(
                environmentGate = "CRAFTLESS_REAL_CLIENT_SMOKE",
                steps =
                    listOf(
                        SmokeStep(SmokeStepKind.START_SERVER, "Start local offline Paper server"),
                        SmokeStep(SmokeStepKind.LAUNCH_CLIENT, "Launch one offline real Minecraft Java client"),
                        SmokeStep(SmokeStepKind.START_API, "Start Craftless local API wrapper"),
                        SmokeStep(SmokeStepKind.CONNECT_CLIENT, "Connect client through Craftless API"),
                        SmokeStep(SmokeStepKind.ASSERT_SERVER_JOIN, "Assert server saw player join"),
                        SmokeStep(SmokeStepKind.COLLECT_ARTIFACTS, "Collect logs, events, OpenAPI, and metadata"),
                    ),
                artifacts =
                    listOf(
                        "openapi.json",
                        "calls.jsonl",
                        "events.jsonl",
                        "stdout.log",
                        "stderr.log",
                        "version.json",
                        "session.json",
                    ),
            )
    }
}

data class SmokeStep(
    val kind: SmokeStepKind,
    val description: String,
)

enum class SmokeStepKind {
    START_SERVER,
    LAUNCH_CLIENT,
    START_API,
    CONNECT_CLIENT,
    ASSERT_SERVER_JOIN,
    COLLECT_ARTIFACTS,
}
