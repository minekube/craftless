plugins {
    id("net.fabricmc.fabric-loom-remap")
}

dependencies {
    "minecraft"("com.mojang:minecraft:1.21.6")
    "mappings"("net.fabricmc:yarn:1.21.6+build.1:v2")
    "modImplementation"("net.fabricmc:fabric-loader:0.19.3")

    implementation(project(":driver-api"))
    implementation(project(":driver-runtime"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.register("fabricClientSmoke") {
    group = "verification"
    description = "Opt-in Fabric real-client smoke. Set CRAFTLESS_FABRIC_CLIENT_SMOKE=1 to run the local server smoke and Loom runClient."

    val enabled = System.getenv("CRAFTLESS_FABRIC_CLIENT_SMOKE") == "1" ||
        System.getenv("CRAFTLESS_FABRIC_CLIENT_SMOKE").equals("true", ignoreCase = true)

    if (enabled) {
        dependsOn(":testkit:localMinecraftServerSmoke", "runClient")
    }

    doLast {
        if (enabled) {
            println("Fabric client smoke requested through CRAFTLESS_FABRIC_CLIENT_SMOKE")
        } else {
            println("set CRAFTLESS_FABRIC_CLIENT_SMOKE=1 to run the Fabric client smoke")
        }
    }
}
