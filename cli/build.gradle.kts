import groovy.json.JsonSlurper
import java.nio.file.Path

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
val fabricDriverArtifactStagingDir = layout.buildDirectory.dir("generated/driver-lane-artifacts")

gradle.projectsEvaluated {
    val fabricDriverRemapJar = fabricDriverProject.tasks.named("remapJar")
    val fabricDriverLaneCatalogTask = fabricDriverProject.tasks.named("writeFabricDriverLaneCatalog")
    val fabricDriverLaneCatalog =
        fabricDriverProject.layout.buildDirectory.file("generated/driver-lanes/fabric-driver-lanes.json")
    val driverModManifest =
        tasks.register("writeDriverModManifest") {
            val outputFile = layout.buildDirectory.file("generated/driver-mods/driver-mods.json")
            dependsOn(fabricDriverLaneCatalogTask)
            inputs.file(fabricDriverLaneCatalog)
            outputs.file(outputFile)

            doLast {
                val output = outputFile.get().asFile
                val catalog = fabricDriverLaneCatalog.get().asFile
                output.parentFile.mkdirs()
                output.writeText(catalog.readText().trimEnd() + "\n")
            }
        }
    val stageFabricDriverLaneArtifacts =
        tasks.register("stageFabricDriverLaneArtifacts") {
            dependsOn(fabricDriverLaneCatalogTask)
            dependsOn(fabricDriverRemapJar)
            inputs.file(fabricDriverLaneCatalog)
            inputs.files(fabricDriverRemapJar)
            outputs.dir(fabricDriverArtifactStagingDir)

            doLast {
                val outputRoot = fabricDriverArtifactStagingDir.get().asFile
                outputRoot.deleteRecursively()
                val catalog = JsonSlurper().parse(fabricDriverLaneCatalog.get().asFile) as Map<*, *>
                val entries = catalog["entries"] as? List<*> ?: error("Fabric driver lane catalog entries must be a list")
                entries.forEach { rawEntry ->
                    val entry = rawEntry as? Map<*, *> ?: error("Fabric driver lane catalog entry must be an object")
                    val artifactKey = entry["artifactKey"]?.toString() ?: error("Fabric driver lane entry requires artifactKey")
                    val distributionPath =
                        entry["distributionPath"]?.toString()
                            ?: error("Fabric driver lane entry requires distributionPath")
                    val relativePath = Path.of(distributionPath)
                    require(!relativePath.isAbsolute && relativePath.normalize() == relativePath) {
                        "Fabric driver lane distributionPath must be a relative normalized path: $distributionPath"
                    }
                    val source =
                        when (artifactKey) {
                            "fabric-current-remap-jar" ->
                                fabricDriverRemapJar
                                    .get()
                                    .outputs
                                    .files
                                    .singleFile

                            else -> error("Unsupported Fabric driver lane artifactKey: $artifactKey")
                        }
                    val target = outputRoot.toPath().resolve(relativePath).toFile()
                    target.parentFile.mkdirs()
                    source.copyTo(target, overwrite = true)
                }
            }
        }

    distributions {
        main {
            contents {
                from(driverModManifest)
                from(stageFabricDriverLaneArtifacts)
            }
        }
    }

    tasks.named("distZip") {
        dependsOn(stageFabricDriverLaneArtifacts)
        dependsOn(driverModManifest)
    }

    tasks.named("distTar") {
        dependsOn(stageFabricDriverLaneArtifacts)
        dependsOn(driverModManifest)
    }

    tasks.named("installDist") {
        dependsOn(stageFabricDriverLaneArtifacts)
        dependsOn(driverModManifest)
    }
}
