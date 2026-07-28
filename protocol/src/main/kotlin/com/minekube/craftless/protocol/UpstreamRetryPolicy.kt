package com.minekube.craftless.protocol

/**
 * Upstream response statuses that represent transient conditions for idempotent
 * metadata and artifact GETs.
 *
 * 429 is rate-limiting, the exact upstream CDN condition that burned the canary
 * on 2026-07-28, so it must remain retryable. 408 and 425 are transient by HTTP
 * semantics, so this allowlist describes what those statuses mean rather than
 * encoding a judgement that could drift over time.
 */
val RETRYABLE_UPSTREAM_STATUS_CODES: Set<Int> = setOf(408, 425, 429, 500, 502, 503, 504)
