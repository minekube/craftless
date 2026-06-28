plugins {
    application
}

dependencies {
    implementation(project(":protocol"))
    implementation(project(":daemon"))
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.ktor:ktor-client-core-jvm:3.5.0")
    implementation("io.ktor:ktor-client-cio-jvm:3.5.0")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.17")

    testImplementation(project(":driver-api"))
    testImplementation(project(":testkit"))
    testImplementation("io.ktor:ktor-server-core-jvm:3.5.0")
    testImplementation("io.ktor:ktor-server-cio-jvm:3.5.0")
}

application {
    applicationName = "craftless"
    mainClass.set("com.minekube.craftless.cli.MainKt")
}

val fabricDriverProject = project(":driver-fabric")

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

gradle.projectsEvaluated {
    val fabricDriverRemapJar = fabricDriverProject.tasks.named("remapJar")
    val fabricMinecraftVersion =
        fabricDriverProject.extensions.extraProperties["fabricCompiledMinecraftVersion"].toString()
    val fabricLoaderVersion =
        fabricDriverProject.extensions.extraProperties["fabricCompiledLoaderVersion"].toString()
    val driverModManifest =
        tasks.register("writeDriverModManifest") {
            val outputFile = layout.buildDirectory.file("generated/driver-mods/driver-mods.json")
            inputs.property("fabricMinecraftVersion", fabricMinecraftVersion)
            inputs.property("fabricLoaderVersion", fabricLoaderVersion)
            outputs.file(outputFile)

            doLast {
                val output = outputFile.get().asFile
                output.parentFile.mkdirs()
                output.writeText(
                    """
                    {
                      "entries": [
                        {
                          "loader": "FABRIC",
                          "minecraftVersion": ${jsonString(fabricMinecraftVersion)},
                          "loaderVersion": ${jsonString(fabricLoaderVersion)},
                          "path": "mods/craftless-driver-fabric.jar"
                        }
                      ]
                    }
                    """.trimIndent() + "\n",
                )
            }
        }

    distributions {
        main {
            contents {
                from(driverModManifest)
                into("mods") {
                    from(fabricDriverRemapJar) {
                        rename { "craftless-driver-fabric.jar" }
                    }
                }
            }
        }
    }

    tasks.named("distZip") {
        dependsOn(fabricDriverRemapJar)
        dependsOn(driverModManifest)
    }

    tasks.named("distTar") {
        dependsOn(fabricDriverRemapJar)
        dependsOn(driverModManifest)
    }

    tasks.named("installDist") {
        dependsOn(fabricDriverRemapJar)
        dependsOn(driverModManifest)
    }
}
