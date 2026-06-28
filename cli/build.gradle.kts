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

gradle.projectsEvaluated {
    val fabricDriverRemapJar = fabricDriverProject.tasks.named("remapJar")

    distributions {
        main {
            contents {
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
    }

    tasks.named("distTar") {
        dependsOn(fabricDriverRemapJar)
    }

    tasks.named("installDist") {
        dependsOn(fabricDriverRemapJar)
    }
}
