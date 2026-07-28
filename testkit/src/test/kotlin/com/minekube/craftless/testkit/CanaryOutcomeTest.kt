package com.minekube.craftless.testkit

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanaryOutcomeTest {
    @Test
    fun `mojang asset cdn connect timeout is infrastructure`() {
        // Verbatim body of the 2026-07-28 canary's clients-create-latest-release.log.
        val failure =
            """{"code":"BAD_REQUEST","message":"Connect timeout has expired """ +
                """[url=https://resources.download.minecraft.net/f0/f066613bca60316fdc9ae333e39c1c9fd8a06e4de, """ +
                """connect_timeout=15000 ms]"}"""

        assertEquals(CanaryFailureClass.INFRASTRUCTURE, CanaryFailureClassifier.classify(failure))
        assertEquals("upstream-connect-timeout", CanaryFailureClassifier.infrastructureSignature(failure)?.id)
    }

    @Test
    fun `unsupported version break is a product failure despite the same error code`() {
        // A real version break returns the same BAD_REQUEST code as the CDN timeout above.
        // Only the message distinguishes them, so the classifier must key on the message.
        val failure =
            """{"code":"BAD_REQUEST","message":"driver mod manifest has no Fabric entry for 26.3"}"""

        assertEquals(CanaryFailureClass.PRODUCT, CanaryFailureClassifier.classify(failure))
        assertNull(CanaryFailureClassifier.infrastructureSignature(failure))
    }

    @Test
    fun `unrecognised failures default to product`() {
        listOf(
            "generated action world.time.query did not return ACCEPTED",
            "timed out waiting for client.attached for latest-current",
            "timed out waiting for connected generated OpenAPI for latest-current",
            "expected player join evidence for LatestCurrent",
            "minecraft smoke action command exited with 1",
        ).forEach { failure ->
            assertEquals(
                CanaryFailureClass.PRODUCT,
                CanaryFailureClassifier.classify(failure),
                "expected product classification for: $failure",
            )
        }
    }

    @Test
    fun `transient transport faults against upstream hosts are infrastructure`() {
        mapOf(
            "java.net.UnknownHostException: piston-meta.mojang.com" to "dns-resolution-failure",
            "Socket timeout has expired [url=https://libraries.minecraft.net/x.jar]" to "upstream-socket-timeout",
            "javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake" to "tls-handshake-failure",
            "metadata fetch failed for https://meta.fabricmc.net/v2/versions/loader: 503" to "upstream-server-error",
            "artifact fetch failed for https://resources.download.minecraft.net/aa/bb: Connection reset" to
                "upstream-transport-fault",
        ).forEach { (failure, expectedSignature) ->
            assertEquals(
                CanaryFailureClass.INFRASTRUCTURE,
                CanaryFailureClassifier.classify(failure),
                "expected infrastructure classification for: $failure",
            )
            assertEquals(expectedSignature, CanaryFailureClassifier.infrastructureSignature(failure)?.id)
        }
    }

    @Test
    fun `a missing upstream artifact stays a product failure`() {
        // 404 from an upstream host is a real answer about the version, not a hiccup.
        val failure = "artifact fetch failed for https://resources.download.minecraft.net/aa/bb: 404"

        assertEquals(CanaryFailureClass.PRODUCT, CanaryFailureClassifier.classify(failure))
    }

    @Test
    fun `failed outcome requires a classification`() {
        assertFailsWith<IllegalArgumentException> {
            CanaryOutcome(verdict = CanaryVerdict.FAIL, reason = "broken", phase = CanaryPhase.PRODUCT)
        }
        assertFailsWith<IllegalArgumentException> {
            CanaryOutcome(
                verdict = CanaryVerdict.FAIL,
                failureClass = CanaryFailureClass.PRODUCT,
                phase = CanaryPhase.PRODUCT,
                reason = "  ",
            )
        }
    }

    @Test
    fun `outcome round trips through its document`() {
        val root = createTempDirectory("craftless-canary-outcome")
        val outcome =
            CanaryOutcome(
                verdict = CanaryVerdict.PASS,
                minecraftVersion = "latest-release",
                evidenceCount = 2,
                cleanupFailure = "minecraft server did not stop after 10000ms; forcibly terminated during teardown",
            )

        val written = outcome.write(root.resolve("artifacts").resolve(CanaryOutcome.FILE_NAME))

        assertEquals(outcome, CanaryOutcome.read(written))
        assertTrue(written.endsWith(CanaryOutcome.FILE_NAME))
    }
}
