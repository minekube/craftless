import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.jvm.tasks.Jar
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
val officialFabricDriverProject = project(":driver-fabric-official")
val fabricDriverArtifactStagingDir = layout.buildDirectory.dir("generated/driver-lane-artifacts")
val extraFabricDriverLaneRoot =
    providers.gradleProperty("craftless.extraFabricDriverLaneRoot").map { path ->
        rootProject.layout.projectDirectory.dir(path)
    }

fun catalogEntries(catalog: Map<*, *>): List<Map<*, *>> {
    val entries = catalog["entries"] as? List<*> ?: error("Fabric driver lane catalog entries must be a list")
    return entries.map { rawEntry ->
        rawEntry as? Map<*, *> ?: error("Fabric driver lane catalog entry must be an object")
    }
}

fun requiredCatalogString(
    entry: Map<*, *>,
    field: String,
): String =
    entry[field]
        ?.toString()
        ?.takeIf { value -> value.isNotBlank() }
        ?: error("Fabric driver lane entry requires $field")

fun requiredCatalogInt(
    entry: Map<*, *>,
    field: String,
): Int =
    entry[field]
        ?.toString()
        ?.toIntOrNull()
        ?: error("Fabric driver lane entry requires numeric $field")

fun optionalCatalogStringList(
    entry: Map<*, *>,
    field: String,
): List<String> {
    val values = entry[field] ?: return emptyList()
    return (values as? List<*>)
        ?.map { value ->
            value
                ?.toString()
                ?.takeIf { string -> string.isNotBlank() }
                ?: error("Fabric driver lane $field entries must be non-blank strings")
        }
        ?: error("Fabric driver lane $field must be a list")
}

fun validatedDistributionPath(distributionPath: String): String {
    val relativePath = Path.of(distributionPath)
    require(!relativePath.isAbsolute && relativePath.normalize() == relativePath) {
        "Fabric driver lane distributionPath must be a relative normalized path: $distributionPath"
    }
    return distributionPath
}

fun extraFabricDriverLaneCatalogFiles(root: File?): List<File> =
    root
        ?.takeIf { candidate -> candidate.isDirectory }
        ?.walkTopDown()
        ?.filter { file -> file.isFile && file.name == "fabric-driver-lanes.json" }
        ?.sortedBy { file -> file.relativeTo(root).invariantSeparatorsPath }
        ?.toList()
        .orEmpty()

fun mergedCatalogEntries(
    primaryCatalog: File,
    builtInCatalogs: List<File>,
    extraRoot: File?,
): List<Map<*, *>> {
    val primary = JsonSlurper().parse(primaryCatalog) as Map<*, *>
    val builtIns =
        builtInCatalogs.flatMap { catalogFile ->
            catalogEntries(JsonSlurper().parse(catalogFile) as Map<*, *>)
        }
    val extras =
        extraFabricDriverLaneCatalogFiles(extraRoot).flatMap { catalogFile ->
            catalogEntries(JsonSlurper().parse(catalogFile) as Map<*, *>)
        }
    return catalogEntries(primary) + builtIns + extras
}

fun renderDriverModManifest(entries: List<Map<*, *>>): String {
    val manifestEntries =
        entries.map { entry ->
            linkedMapOf<String, Any>(
                "loader" to requiredCatalogString(entry, "loader"),
                "minecraftVersion" to requiredCatalogString(entry, "minecraftVersion"),
                "loaderVersion" to requiredCatalogString(entry, "loaderVersion"),
                "fabricApiVersion" to requiredCatalogString(entry, "fabricApiVersion"),
                "javaMajorVersion" to requiredCatalogInt(entry, "javaMajorVersion"),
                "mappingsFingerprint" to requiredCatalogString(entry, "mappingsFingerprint"),
                "path" to validatedDistributionPath(requiredCatalogString(entry, "distributionPath")),
            ).also { manifestEntry ->
                val runtimeMods =
                    optionalCatalogStringList(entry, "runtimeMods")
                        .map(::validatedDistributionPath)
                if (runtimeMods.isNotEmpty()) {
                    manifestEntry["runtimeMods"] = runtimeMods
                }
            }
        }
    return JsonOutput.prettyPrint(JsonOutput.toJson(linkedMapOf("entries" to manifestEntries))) + "\n"
}

fun stagedExtraLaneArtifact(
    extraRoot: File?,
    distributionPath: String,
): File? =
    extraRoot
        ?.toPath()
        ?.resolve(distributionPath)
        ?.normalize()
        ?.toFile()
        ?.takeIf { file -> file.isFile }

fun stageDistributionPath(
    source: File,
    outputRoot: File,
    distributionPath: String,
) {
    val target = outputRoot.toPath().resolve(distributionPath).toFile()
    target.parentFile.mkdirs()
    source.copyTo(target, overwrite = true)
}

gradle.projectsEvaluated {
    val fabricDriverRemapJar = fabricDriverProject.tasks.named("remapJar")
    val fabricDriverLaneCatalogTask = fabricDriverProject.tasks.named("writeFabricDriverLaneCatalog")
    val fabricDriverLaneCatalog =
        fabricDriverProject.layout.buildDirectory.file("generated/driver-lanes/fabric-driver-lanes.json")
    val officialFabricDriverJar = officialFabricDriverProject.tasks.named<Jar>("jar")
    val officialFabricDriverLaneCatalogTask = officialFabricDriverProject.tasks.named("writeOfficialFabricDriverLaneCatalog")
    val officialFabricDriverLaneCatalog =
        officialFabricDriverProject.layout.buildDirectory.file("generated/driver-lanes/fabric-driver-lanes.json")
    val configuredExtraFabricDriverLaneRoot = extraFabricDriverLaneRoot.orNull?.asFile
    val driverModManifest =
        tasks.register("writeDriverModManifest") {
            val outputFile = layout.buildDirectory.file("generated/driver-mods/driver-mods.json")
            dependsOn(fabricDriverLaneCatalogTask)
            dependsOn(officialFabricDriverLaneCatalogTask)
            inputs.file(fabricDriverLaneCatalog)
            inputs.file(officialFabricDriverLaneCatalog)
            configuredExtraFabricDriverLaneRoot?.let { root -> inputs.dir(root).optional() }
            outputs.file(outputFile)

            doLast {
                val output = outputFile.get().asFile
                val entries =
                    mergedCatalogEntries(
                        fabricDriverLaneCatalog.get().asFile,
                        listOf(officialFabricDriverLaneCatalog.get().asFile),
                        configuredExtraFabricDriverLaneRoot,
                    )
                output.parentFile.mkdirs()
                output.writeText(renderDriverModManifest(entries))
            }
        }
    val stageFabricDriverLaneArtifacts =
        tasks.register("stageFabricDriverLaneArtifacts") {
            dependsOn(fabricDriverLaneCatalogTask)
            dependsOn(fabricDriverRemapJar)
            dependsOn(officialFabricDriverLaneCatalogTask)
            dependsOn(officialFabricDriverJar)
            inputs.file(fabricDriverLaneCatalog)
            inputs.file(officialFabricDriverLaneCatalog)
            inputs.files(fabricDriverRemapJar)
            inputs.files(officialFabricDriverJar)
            configuredExtraFabricDriverLaneRoot?.let { root -> inputs.dir(root).optional() }
            outputs.dir(fabricDriverArtifactStagingDir)

            doLast {
                val outputRoot = fabricDriverArtifactStagingDir.get().asFile
                outputRoot.deleteRecursively()
                val entries =
                    mergedCatalogEntries(
                        fabricDriverLaneCatalog.get().asFile,
                        listOf(officialFabricDriverLaneCatalog.get().asFile),
                        configuredExtraFabricDriverLaneRoot,
                    )
                entries.forEach { entry ->
                    val artifactKey = requiredCatalogString(entry, "artifactKey")
                    val distributionPath = validatedDistributionPath(requiredCatalogString(entry, "distributionPath"))
                    val source =
                        when (artifactKey) {
                            "fabric-current-remap-jar" ->
                                fabricDriverRemapJar
                                    .get()
                                    .outputs
                                    .files
                                    .singleFile

                            "fabric-26-2-official-jar" ->
                                officialFabricDriverJar
                                    .get()
                                    .outputs
                                    .files
                                    .singleFile

                            else ->
                                stagedExtraLaneArtifact(configuredExtraFabricDriverLaneRoot, distributionPath)
                                    ?: error("Unsupported Fabric driver lane artifactKey: $artifactKey")
                        }
                    stageDistributionPath(source, outputRoot, distributionPath)
                    optionalCatalogStringList(entry, "runtimeMods")
                        .map(::validatedDistributionPath)
                        .forEach { runtimeModPath ->
                            val runtimeMod =
                                stagedExtraLaneArtifact(configuredExtraFabricDriverLaneRoot, runtimeModPath)
                                    ?: error("Fabric driver lane runtime mod is missing: $runtimeModPath")
                            stageDistributionPath(runtimeMod, outputRoot, runtimeModPath)
                        }
                }
            }
        }
    val verifyInstallDistDriverLanes =
        tasks.register("verifyInstallDistDriverLanes") {
            dependsOn("installDist")
            inputs.file(layout.buildDirectory.file("install/craftless/driver-mods.json"))
            inputs.file(layout.buildDirectory.file("install/craftless/mods/fabric-26.2/craftless-driver-fabric-official.jar"))

            doLast {
                val installRoot =
                    layout.buildDirectory
                        .dir("install/craftless")
                        .get()
                        .asFile
                        .toPath()
                val manifest = installRoot.resolve("driver-mods.json").toFile()
                check(manifest.isFile) { "installed Craftless distribution is missing driver-mods.json" }
                val entries = catalogEntries(JsonSlurper().parse(manifest) as Map<*, *>)
                val versions = entries.map { entry -> requiredCatalogString(entry, "minecraftVersion") }.toSet()
                check("1.21.6" in versions) { "installed Craftless distribution is missing the current Fabric driver lane" }
                check("26.2" in versions) { "installed Craftless distribution is missing the latest official Fabric driver lane" }
                check(installRoot.resolve("mods/fabric-26.2/craftless-driver-fabric-official.jar").toFile().isFile) {
                    "installed Craftless distribution is missing the latest official Fabric driver jar"
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

    tasks.named("check") {
        dependsOn(verifyInstallDistDriverLanes)
    }
}
