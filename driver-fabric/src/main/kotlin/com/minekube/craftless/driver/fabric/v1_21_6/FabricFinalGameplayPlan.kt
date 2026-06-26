package com.minekube.craftless.driver.fabric.v1_21_6

data class FabricFinalGameplayPlan(
    val environmentGate: String,
    val minecraftVersion: String,
    val gradleTasks: List<String>,
    val artifactsDirectory: String,
    val steps: List<FabricFinalGameplayStep>,
    val artifacts: List<String>,
    val completionGates: List<String>,
) {
    companion object {
        fun default(): FabricFinalGameplayPlan =
            FabricFinalGameplayPlan(
                environmentGate = "CRAFTLESS_FINAL_GAMEPLAY",
                minecraftVersion = "1.21.6",
                gradleTasks = listOf(":driver-fabric:fabricFinalGameplay"),
                artifactsDirectory = "driver-fabric/build/craftless-final-gameplay/artifacts",
                steps =
                    listOf(
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.START_LOCAL_SERVER,
                            "Start the local Minecraft server fixture and keep it alive while the Fabric client runs",
                        ),
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.LAUNCH_VISIBLE_FABRIC_CLIENT,
                            "Launch the Craftless-controlled visible Fabric client through Gradle runClient",
                        ),
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.FETCH_GRAPH_OPENAPI,
                            "Fetch connected per-client OpenAPI, action, and resource projections generated from the runtime graph",
                        ),
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.SUBSCRIBE_SSE,
                            "Capture the per-client SSE event stream as evidence of live observations",
                        ),
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.INVOKE_DISCOVERED_GAMEPLAY,
                            "Invoke available discovered actions through the generic client run endpoint",
                        ),
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.INVITE_ROBIN,
                            "Use macOS say to ask Robin to join or observe the server session",
                        ),
                        FabricFinalGameplayStep(
                            FabricFinalGameplayStepKind.WAIT_FOR_ROBIN_CHAT_CONFIRMATION,
                            "Keep the session open until Robin writes in Minecraft chat that the goal may be completed",
                        ),
                    ),
                artifacts =
                    listOf(
                        "server.log",
                        "server-evidence.jsonl",
                        "client-openapi-connected.json",
                        "client-actions-connected.json",
                        "client-resources-connected.json",
                        "client-events.jsonl",
                        "client-events-stream.sse",
                        "gameplay-results.jsonl",
                        "runtime-metadata.json",
                    ),
                completionGates =
                    listOf(
                        "Graph-generated OpenAPI and action/resource projections are captured.",
                        "SSE evidence is captured from /clients/{id}/events:stream.",
                        "No static fallback bypass is used for gameplay breadth.",
                        "Robin writes in Minecraft chat that the goal may be completed.",
                    ),
            )
    }
}

data class FabricFinalGameplayStep(
    val kind: FabricFinalGameplayStepKind,
    val description: String,
)

enum class FabricFinalGameplayStepKind {
    START_LOCAL_SERVER,
    LAUNCH_VISIBLE_FABRIC_CLIENT,
    FETCH_GRAPH_OPENAPI,
    SUBSCRIBE_SSE,
    INVOKE_DISCOVERED_GAMEPLAY,
    INVITE_ROBIN,
    WAIT_FOR_ROBIN_CHAT_CONFIRMATION,
}
