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
