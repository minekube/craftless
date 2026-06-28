plugins {
    id("net.fabricmc.fabric-loom")
}

fun jsonString(value: String): String =
    buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }

fun jsonStringArray(values: List<String>): String = values.joinToString(prefix = "[", postfix = "]") { jsonString(it) }

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
    implementation(project(":driver-fabric-discovery"))
    include(project(":protocol"))
    include(project(":driver-api"))
    include(project(":driver-runtime"))
    include(project(":driver-fabric-attach"))
    include(project(":driver-fabric-discovery"))
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
    testImplementation(project(":daemon"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("io.ktor:ktor-client-core-jvm:3.5.0")
    testImplementation("io.ktor:ktor-client-cio-jvm:3.5.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.register<JavaExec>("officialFabricAttachProbe") {
    group = "verification"
    description = "Opt-in official Fabric latest/current launch and self-attach probe."
    dependsOn("testClasses")
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.minekube.craftless.driver.fabric.official.probe.OfficialFabricAttachProbeKt")

    val rootProjectPath = rootProject.projectDir.absolutePath
    val defaultClientCommand =
        listOf(
            "mise",
            "-C",
            rootProjectPath,
            "exec",
            "java@temurin-25.0.3+9.0.LTS",
            "gradle@9.6.0",
            "--",
            "gradle",
            "-p",
            rootProjectPath,
            ":driver-fabric-official:runClient",
        )
    environment(
        "CRAFTLESS_OFFICIAL_ATTACH_PROBE_ARTIFACTS_DIR",
        layout.buildDirectory
            .dir("craftless-official-attach-probe")
            .get()
            .asFile
            .absolutePath,
    )
    environment(
        "CRAFTLESS_OFFICIAL_ATTACH_PROBE_CLIENT_COMMAND_JSON",
        System.getenv("CRAFTLESS_OFFICIAL_ATTACH_PROBE_CLIENT_COMMAND_JSON")
            ?: jsonStringArray(defaultClientCommand),
    )
    environment(
        "CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE",
        System.getenv("CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE").orEmpty(),
    )
    System.getenv("CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS")?.takeIf { it.isNotBlank() }?.let { timeout ->
        environment("CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS", timeout)
    }
}
