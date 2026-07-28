package com.minekube.craftless.testkit

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING

/**
 * Which stage of a probe run produced a failure.
 *
 * The distinction exists so a probe result can say *where* it broke, not just
 * that it broke. Only [PRODUCT] failures are statements about Craftless.
 */
@Serializable
enum class CanaryPhase {
    /** Provisioning and starting the harness itself: server jar download, server startup. */
    SETUP,

    /** Everything the probe asserts about Craftless: create, attach, connect, invoke, evidence. */
    PRODUCT,

    /** Post-assertion cleanup. Never a statement about Craftless. */
    TEARDOWN,
}

/**
 * What a red probe run actually means.
 *
 * [PRODUCT] is the alarm this canary exists to raise. [INFRASTRUCTURE] means the
 * canary could not obtain a verdict — an upstream CDN, DNS, TLS or runner
 * problem — and is explicitly *not* evidence that the supported Minecraft lane
 * broke.
 */
@Serializable
enum class CanaryFailureClass {
    PRODUCT,
    INFRASTRUCTURE,
}

@Serializable
enum class CanaryVerdict {
    PASS,
    FAIL,
}

/**
 * Machine-readable outcome of one canary run.
 *
 * This is the document CI reports from. It is written on every run, passing or
 * failing, so a green run is as legible as a red one.
 */
@Serializable
data class CanaryOutcome(
    val schemaVersion: Int = 1,
    val verdict: CanaryVerdict,
    val failureClass: CanaryFailureClass? = null,
    val phase: CanaryPhase? = null,
    val reason: String? = null,
    /** Id of the transient-infrastructure signature that matched, when one did. */
    val signature: String? = null,
    val minecraftVersion: String? = null,
    val evidenceCount: Int = 0,
    /**
     * A teardown failure that did **not** change [verdict].
     *
     * Cleanup runs after every product assertion has already been decided, so it
     * can never invert a pass. It is surfaced here rather than swallowed.
     */
    val cleanupFailure: String? = null,
    /** Log files worth reading when diagnosing this outcome. */
    val diagnostics: List<String> = emptyList(),
) {
    init {
        require(schemaVersion > 0) { "canary outcome schema version must be positive" }
        if (verdict == CanaryVerdict.FAIL) {
            require(failureClass != null) { "failed canary outcome requires a failure class" }
            require(phase != null) { "failed canary outcome requires a phase" }
            require(!reason.isNullOrBlank()) { "failed canary outcome requires a reason" }
        } else {
            require(failureClass == null) { "passing canary outcome must not declare a failure class" }
        }
        require(evidenceCount >= 0) { "canary outcome evidence count must not be negative" }
    }

    fun write(destination: Path): Path {
        Files.createDirectories(destination.parent)
        Files.writeString(destination, canaryOutcomeJson.encodeToString(this) + "\n", CREATE, TRUNCATE_EXISTING)
        return destination
    }

    companion object {
        const val FILE_NAME: String = "canary-outcome.json"

        fun read(source: Path): CanaryOutcome = canaryOutcomeJson.decodeFromString(Files.readString(source))
    }
}

/**
 * A failure text pattern that identifies a transient infrastructure fault.
 *
 * Signatures are deliberately narrow. Anything that does not match one is
 * classified as a product failure — see [CanaryFailureClassifier].
 */
data class CanaryInfrastructureSignature(
    val id: String,
    val description: String,
    private val pattern: Regex,
) {
    fun matches(text: String): Boolean = pattern.containsMatchIn(text)
}

/**
 * Decides whether a failure text describes broken infrastructure or broken product.
 *
 * **The default is [CanaryFailureClass.PRODUCT].** An unrecognised failure is
 * reported as a product failure, because the cost of a false product alarm is a
 * wasted investigation while the cost of a false infrastructure label is a
 * silently ignored version break. Only failures matching an explicit,
 * narrowly-scoped transient signature are downgraded to infrastructure.
 *
 * Signatures never key on the API error code alone: a Mojang CDN timeout and a
 * genuine "no driver mod for this version" break are both surfaced as
 * `BAD_REQUEST`, so only the message body can tell them apart.
 */
object CanaryFailureClassifier {
    private const val UPSTREAM_HOSTS =
        "piston-meta\\.mojang\\.com|piston-data\\.mojang\\.com|launchermeta\\.mojang\\.com|" +
            "resources\\.download\\.minecraft\\.net|libraries\\.minecraft\\.net|" +
            "meta\\.fabricmc\\.net|maven\\.fabricmc\\.net|repo1\\.maven\\.org|services\\.gradle\\.org"

    private const val RETRYABLE_STATUS_CODES = "429|500|502|503|504"

    private const val TRANSPORT_FAULTS =
        "Connection reset|Connection refused|Broken pipe|Read timed out|" +
            "No route to host|Network is unreachable|Premature end of Content-Length"

    /**
     * Ordered so the most specific signature wins when several could match.
     */
    val signatures: List<CanaryInfrastructureSignature> =
        listOf(
            CanaryInfrastructureSignature(
                id = "upstream-connect-timeout",
                description = "TCP connect to an upstream artifact/metadata host timed out",
                pattern = Regex("Connect timeout has expired"),
            ),
            CanaryInfrastructureSignature(
                id = "upstream-socket-timeout",
                description = "Socket read from an upstream artifact/metadata host timed out",
                pattern = Regex("Socket timeout has expired"),
            ),
            CanaryInfrastructureSignature(
                id = "upstream-request-timeout",
                description = "HTTP request to an upstream artifact/metadata host timed out",
                pattern = Regex("Request timeout has expired"),
            ),
            CanaryInfrastructureSignature(
                id = "dns-resolution-failure",
                description = "Upstream hostname could not be resolved",
                pattern = Regex("UnknownHostException|Temporary failure in name resolution"),
            ),
            CanaryInfrastructureSignature(
                id = "tls-handshake-failure",
                description = "TLS handshake with an upstream host failed",
                pattern = Regex("SSLHandshakeException|SSLPeerUnverifiedException|SSLException"),
            ),
            CanaryInfrastructureSignature(
                id = "upstream-server-error",
                description = "Upstream host answered with a retryable server error status",
                pattern =
                    Regex(
                        "(?:$UPSTREAM_HOSTS)[^\\n]*\\b(?:$RETRYABLE_STATUS_CODES)\\b|" +
                            "\\b(?:$RETRYABLE_STATUS_CODES)\\b[^\\n]*(?:$UPSTREAM_HOSTS)",
                    ),
            ),
            CanaryInfrastructureSignature(
                id = "upstream-transport-fault",
                description = "Connection to an upstream host was refused, reset or truncated",
                pattern = Regex("(?:$UPSTREAM_HOSTS)[^\\n]*(?:$TRANSPORT_FAULTS)|(?:$TRANSPORT_FAULTS)[^\\n]*(?:$UPSTREAM_HOSTS)"),
            ),
        )

    /**
     * Returns the transient signature matching [text], or `null` when the text
     * should be treated as a product failure.
     */
    fun infrastructureSignature(text: String): CanaryInfrastructureSignature? =
        signatures.firstOrNull { signature -> signature.matches(text) }

    fun classify(text: String): CanaryFailureClass =
        if (infrastructureSignature(text) != null) {
            CanaryFailureClass.INFRASTRUCTURE
        } else {
            CanaryFailureClass.PRODUCT
        }
}

private val canaryOutcomeJson =
    Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
