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
val fabricDriverModDistributionPath = "mods/craftless-driver-fabric.jar"
val fabricDriverModDistributionFileName = fabricDriverModDistributionPath.substringAfterLast("/")

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

    distributions {
        main {
            contents {
                from(driverModManifest)
                into("mods") {
                    from(fabricDriverRemapJar) {
                        rename { fabricDriverModDistributionFileName }
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
