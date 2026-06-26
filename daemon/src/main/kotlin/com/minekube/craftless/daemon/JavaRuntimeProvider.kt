package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.JavaRuntimeProviderKind
import java.nio.file.Files
import java.nio.file.Path

data class JavaRuntimeDiscoveryContext(
    val configuredExecutables: List<Path> = emptyList(),
    val managedRuntimeRoot: Path? = null,
    val environment: Map<String, String> = System.getenv(),
    val home: Path? = Path.of(System.getProperty("user.home")),
    val systemExecutables: List<Path> = emptyList(),
)

data class JavaRuntimeCandidate(
    val executable: Path,
    val provider: JavaRuntimeProviderKind,
    val managed: Boolean = false,
)

interface JavaRuntimeProvider {
    val kind: JavaRuntimeProviderKind

    fun candidates(context: JavaRuntimeDiscoveryContext): List<JavaRuntimeCandidate>
}

class ConfiguredJavaRuntimeProvider : JavaRuntimeProvider {
    override val kind: JavaRuntimeProviderKind = JavaRuntimeProviderKind.CONFIGURED

    override fun candidates(context: JavaRuntimeDiscoveryContext): List<JavaRuntimeCandidate> =
        context.configuredExecutables.map { executable ->
            JavaRuntimeCandidate(executable = executable, provider = kind)
        }
}

class ManagedCacheJavaRuntimeProvider : JavaRuntimeProvider {
    override val kind: JavaRuntimeProviderKind = JavaRuntimeProviderKind.MANAGED

    override fun candidates(context: JavaRuntimeDiscoveryContext): List<JavaRuntimeCandidate> =
        context.managedRuntimeRoot
            ?.takeIf(Files::isDirectory)
            ?.findJavaExecutables()
            ?.map { executable ->
                JavaRuntimeCandidate(executable = executable, provider = kind, managed = true)
            }.orEmpty()
}

class MiseJavaRuntimeProvider : JavaRuntimeProvider {
    override val kind: JavaRuntimeProviderKind = JavaRuntimeProviderKind.MISE

    override fun candidates(context: JavaRuntimeDiscoveryContext): List<JavaRuntimeCandidate> {
        val roots =
            listOfNotNull(
                context.environment["MISE_DATA_DIR"]?.let { Path.of(it) },
                context.home?.resolve(".local/share/mise"),
            ).map { root -> root.resolve("installs/java") }
        return roots
            .flatMap { root -> root.findJavaExecutablesInChildren() }
            .map { executable -> JavaRuntimeCandidate(executable = executable, provider = kind) }
    }
}

class SystemJavaRuntimeProvider : JavaRuntimeProvider {
    override val kind: JavaRuntimeProviderKind = JavaRuntimeProviderKind.SYSTEM

    override fun candidates(context: JavaRuntimeDiscoveryContext): List<JavaRuntimeCandidate> {
        val javaHome =
            context.environment["JAVA_HOME"]
                ?.takeIf { it.isNotBlank() }
                ?.let { Path.of(it).resolve("bin/java") }
        return (listOfNotNull(javaHome) + context.systemExecutables)
            .distinct()
            .map { executable -> JavaRuntimeCandidate(executable = executable, provider = kind) }
    }
}

private fun Path.findJavaExecutablesInChildren(): List<Path> {
    if (!Files.isDirectory(this)) return emptyList()
    return Files
        .list(this)
        .use { children ->
            children
                .filter(Files::isDirectory)
                .map { child -> child.resolve("bin/java") }
                .filter(Files::exists)
                .toList()
        }
}

private fun Path.findJavaExecutables(): List<Path> {
    val executables = mutableListOf<Path>()
    Files.walk(this).use { paths ->
        paths
            .filter { path -> path.fileName.toString() == "java" }
            .filter { path -> path.parent?.fileName?.toString() == "bin" }
            .forEach { path -> executables.add(path) }
    }
    return executables
}
