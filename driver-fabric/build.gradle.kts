plugins {
    id("net.fabricmc.fabric-loom-remap")
}

dependencies {
    "minecraft"("com.mojang:minecraft:1.21.6")
    "mappings"("net.fabricmc:yarn:1.21.6+build.1:v2")
    "modImplementation"("net.fabricmc:fabric-loader:0.19.3")

    implementation(project(":driver-api"))
    implementation(project(":driver-runtime"))
    implementation(project(":daemon"))
    implementation("io.ktor:ktor-client-core-jvm:3.5.0")
    implementation("io.ktor:ktor-client-cio-jvm:3.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

val testkitSourceSets =
    project(":testkit")
        .extensions
        .getByType<org.gradle.api.tasks.SourceSetContainer>()

tasks.register<JavaExec>("fabricClientSmoke") {
    group = "verification"
    description = "Opt-in Fabric real-client smoke. Set CRAFTLESS_FABRIC_CLIENT_SMOKE=1 " +
        "to keep a local server alive while a client command runs."
    dependsOn(":testkit:classes")
    classpath = testkitSourceSets.named("main").get().runtimeClasspath
    mainClass.set("com.minekube.craftless.testkit.LocalMinecraftServerSmokeKt")

    val fabricSmokeEnabled =
        System.getenv("CRAFTLESS_FABRIC_CLIENT_SMOKE") == "1" ||
            System.getenv("CRAFTLESS_FABRIC_CLIENT_SMOKE").equals("true", ignoreCase = true)
    environment("CRAFTLESS_FABRIC_CLIENT_SMOKE", System.getenv("CRAFTLESS_FABRIC_CLIENT_SMOKE").orEmpty())

    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_SMOKE_ACTION_COMMAND_JSON").isNullOrBlank()) {
        val rootProjectPath =
            rootProject.projectDir.absolutePath
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        environment(
            "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON",
            """["mise","-C","$rootProjectPath","exec","--","gradle","-p","$rootProjectPath",":driver-fabric:runClient"]""",
        )
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE").isNullOrBlank()) {
        environment(
            "CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE",
            System.getenv("CRAFTLESS_FABRIC_SMOKE_CHAT_MESSAGE")
                ?: "hello from Craftless Fabric smoke",
        )
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_SMOKE_EXPECT_DISCONNECT").isNullOrBlank()) {
        environment("CRAFTLESS_SMOKE_EXPECT_DISCONNECT", "1")
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_SMOKE_PROVISION_ITEM_ID").isNullOrBlank()) {
        environment("CRAFTLESS_SMOKE_PROVISION_ITEM_ID", "minecraft:iron_sword")
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_SMOKE_PROVISION_ITEM_NAME").isNullOrBlank()) {
        environment(
            "CRAFTLESS_SMOKE_PROVISION_ITEM_NAME",
            System.getenv("CRAFTLESS_FABRIC_SMOKE_EQUIP_ITEM") ?: "Iron Sword",
        )
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_SMOKE_PROVISION_ITEM_COUNT").isNullOrBlank()) {
        environment("CRAFTLESS_SMOKE_PROVISION_ITEM_COUNT", "1")
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_FABRIC_SMOKE_REQUIRE_EQUIP_ITEM").isNullOrBlank()) {
        environment("CRAFTLESS_FABRIC_SMOKE_REQUIRE_EQUIP_ITEM", "1")
    }
    if (fabricSmokeEnabled && System.getenv("CRAFTLESS_FABRIC_SMOKE_STARTUP_SETTLE_MS").isNullOrBlank()) {
        environment("CRAFTLESS_FABRIC_SMOKE_STARTUP_SETTLE_MS", "3000")
    }

    doFirst {
        if (fabricSmokeEnabled) {
            println(
                "Fabric client smoke requested; server lifecycle is owned by testkit " +
                    "LocalMinecraftServerSmoke.runWithServer",
            )
        }
    }
}

tasks.register<JavaExec>("fabricFinalGameplay") {
    group = "verification"
    description = "Opt-in final Craftless gameplay run. Set CRAFTLESS_FINAL_GAMEPLAY=1 to launch the local server and Fabric client."
    dependsOn(":testkit:classes")
    classpath = testkitSourceSets.named("main").get().runtimeClasspath
    mainClass.set("com.minekube.craftless.testkit.LocalMinecraftServerSmokeKt")

    val finalGameplayEnabled =
        System.getenv("CRAFTLESS_FINAL_GAMEPLAY") == "1" ||
            System.getenv("CRAFTLESS_FINAL_GAMEPLAY").equals("true", ignoreCase = true)
    environment("CRAFTLESS_FINAL_GAMEPLAY", System.getenv("CRAFTLESS_FINAL_GAMEPLAY").orEmpty())

    if (finalGameplayEnabled) {
        val rootProjectPath =
            rootProject.projectDir.absolutePath
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        environment("CRAFTLESS_FABRIC_CLIENT_SMOKE", "1")
        environment(
            "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT",
            layout.buildDirectory
                .dir("craftless-final-gameplay")
                .get()
                .asFile
                .absolutePath,
        )
        environment(
            "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON",
            System.getenv("CRAFTLESS_SMOKE_ACTION_COMMAND_JSON")
                ?: """["mise","-C","$rootProjectPath","exec","--","gradle","-p","$rootProjectPath",":driver-fabric:runClient"]""",
        )
        environment(
            "CRAFTLESS_FABRIC_SMOKE_CHAT_MESSAGE",
            System.getenv("CRAFTLESS_FABRIC_SMOKE_CHAT_MESSAGE")
                ?: "hello from Craftless final gameplay",
        )
        environment(
            "CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE",
            System.getenv("CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE")
                ?: "hello from Craftless final gameplay",
        )
        environment(
            "CRAFTLESS_SMOKE_PROVISION_ITEM_ID",
            System.getenv("CRAFTLESS_SMOKE_PROVISION_ITEM_ID") ?: "minecraft:iron_sword",
        )
        environment(
            "CRAFTLESS_SMOKE_PROVISION_ITEM_NAME",
            System.getenv("CRAFTLESS_SMOKE_PROVISION_ITEM_NAME") ?: "Iron Sword",
        )
        environment(
            "CRAFTLESS_SMOKE_PROVISION_ITEM_COUNT",
            System.getenv("CRAFTLESS_SMOKE_PROVISION_ITEM_COUNT") ?: "1",
        )
        environment(
            "CRAFTLESS_FABRIC_SMOKE_REQUIRE_EQUIP_ITEM",
            System.getenv("CRAFTLESS_FABRIC_SMOKE_REQUIRE_EQUIP_ITEM") ?: "1",
        )
        environment(
            "CRAFTLESS_FABRIC_SMOKE_STARTUP_SETTLE_MS",
            System.getenv("CRAFTLESS_FABRIC_SMOKE_STARTUP_SETTLE_MS") ?: "3000",
        )
        environment(
            "CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS",
            System.getenv("CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS") ?: "600000",
        )
    }

    doFirst {
        if (finalGameplayEnabled) {
            println("Final gameplay requested; artifacts will be written under driver-fabric/build/craftless-final-gameplay/artifacts")
            println("Use macOS say when the automated sequence is ready for Robin to join and confirm in Minecraft chat.")
        }
    }
}
