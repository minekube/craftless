plugins {
    id("net.fabricmc.fabric-loom")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:26.2")
    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation("net.fabricmc.fabric-api:fabric-api:0.153.0+26.2")
    implementation(project(":driver-api"))
    implementation(project(":driver-runtime"))
    implementation(project(":driver-fabric-attach"))
    include(project(":protocol"))
    include(project(":driver-api"))
    include(project(":driver-runtime"))
    include(project(":driver-fabric-attach"))
    include("io.ktor:ktor-client-core-jvm:3.5.0")
    include("io.ktor:ktor-client-cio-jvm:3.5.0")
    include("io.ktor:ktor-server-core-jvm:3.5.0")
    include("io.ktor:ktor-server-cio-jvm:3.5.0")
    include("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    include("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
    include("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
    include("org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.9.0")
    include("org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:0.9.0")
    include("io.ktor:ktor-http-jvm:3.5.0")
    include("io.ktor:ktor-http-cio-jvm:3.5.0")
    include("io.ktor:ktor-utils-jvm:3.5.0")
    include("io.ktor:ktor-io-jvm:3.5.0")
    include("io.ktor:ktor-events-jvm:3.5.0")
    include("io.ktor:ktor-websocket-serialization-jvm:3.5.0")
    include("io.ktor:ktor-serialization-jvm:3.5.0")
    include("io.ktor:ktor-websockets-jvm:3.5.0")
    include("io.ktor:ktor-sse-jvm:3.5.0")
    include("io.ktor:ktor-network-jvm:3.5.0")
    include("io.ktor:ktor-network-tls-jvm:3.5.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
