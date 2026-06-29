package com.minekube.craftless.cli

import com.minekube.craftless.protocol.OpenApiDocument
import com.minekube.craftless.protocol.OpenApiOperation
import com.minekube.craftless.protocol.OpenApiPath
import com.minekube.craftless.protocol.OpenApiRequestBody
import com.minekube.craftless.protocol.OpenApiSchema
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path

internal class ApiCli(
    private val json: Json,
    private val httpClientFactory: (Map<String, String>) -> HttpClient,
) {
    fun run(
        args: List<String>,
        stdout: (String) -> Unit,
        stderr: (String) -> Unit,
        env: Map<String, String>,
    ): Int {
        if (args.isEmpty() || args == listOf("--help") || args == listOf("-h")) {
            stdout(help())
            return 0
        }
        val endpoint = args.firstOrNull { !it.startsWith("-") }
        if (endpoint.isNullOrBlank() || !endpoint.startsWith("/")) {
            stderr("error: usage is api <endpoint> [--api <url>] [-X <method>] [-F key=value] [-f key=value]")
            return 2
        }
        val api = args.optionValue("--api") ?: env["CRAFTLESS"] ?: "http://127.0.0.1:8080"
        val method = args.requestMethod()
        return runCatching {
            kotlinx.coroutines.runBlocking {
                httpClientFactory(env).use { http ->
                    val operation = loadOperation(http, api, endpoint, method)
                    if (args.contains("--help")) {
                        stdout(operation?.help(endpoint, method) ?: "Route: $method ${endpoint.pathOnly()}")
                        return@runBlocking 0
                    }
                    val body = args.requestBody(operation?.operation?.requestBody)
                    val response =
                        if (method == "GET") {
                            http.get("${api.trimEnd('/')}$endpoint") {
                                args.headers().forEach { (name, value) -> header(name, value) }
                            }
                        } else {
                            http.post("${api.trimEnd('/')}$endpoint") {
                                args.headers().forEach { (name, value) -> header(name, value) }
                                if (body != null) {
                                    contentType(ContentType.Application.Json)
                                    setBody(json.encodeToString(body))
                                }
                            }
                        }
                    response.forwardBody(stdout, stderr)
                }
            }
        }.getOrElse { error ->
            stderr("error: ${error.message ?: "api request failed"}")
            2
        }
    }

    fun help(): String =
        buildString {
            appendLine("Usage: craftless api <endpoint> [flags]")
            appendLine()
            appendLine("Invoke Craftless OpenAPI routes by REST endpoint.")
            appendLine()
            appendLine("Flags:")
            appendLine("  -X, --method <method>    HTTP method, GET or POST")
            appendLine("  -F, --field key=value    Add a typed JSON field")
            appendLine("  -f, --raw-field key=value Add a string JSON field")
            appendLine("  -H, --header key:value   Add a request header")
            appendLine("      --input <path>       Read JSON request body from a file")
            appendLine("      --api <url>          Craftless daemon URL")
            appendLine("      --help               Show OpenAPI-derived route help")
        }.trimEnd()

    private suspend fun loadOperation(
        http: HttpClient,
        api: String,
        endpoint: String,
        method: String,
    ): MatchedOperation? {
        val supervisor = http.loadOpenApi("${api.trimEnd('/')}/openapi.json")
        supervisor.matchOperation(endpoint.pathOnly(), method)?.let { return it }
        val clientId = endpoint.clientId() ?: return null
        val client = http.loadOpenApi("${api.trimEnd('/')}/clients/$clientId/openapi.json")
        return client.matchOperation(endpoint.pathOnly(), method)
    }

    private suspend fun HttpClient.loadOpenApi(url: String): OpenApiDocument {
        val response = get(url)
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error(body)
        }
        return json.decodeFromString(body)
    }

    private fun OpenApiDocument.matchOperation(
        endpoint: String,
        method: String,
    ): MatchedOperation? =
        paths.entries.firstNotNullOfOrNull { (path, operations) ->
            val operation = operations.operation(method) ?: return@firstNotNullOfOrNull null
            if (path.matchesEndpoint(endpoint)) {
                MatchedOperation(method = method, path = path, operation = operation)
            } else {
                null
            }
        }

    private fun OpenApiPath.operation(method: String): OpenApiOperation? =
        when (method) {
            "GET" -> get
            "POST" -> post
            else -> null
        }

    private fun String.matchesEndpoint(endpoint: String): Boolean {
        if (this == endpoint) {
            return true
        }
        return Regex("^${toEndpointRegex()}$").matches(endpoint)
    }

    private fun String.toEndpointRegex(): String =
        buildString {
            var index = 0
            while (index < this@toEndpointRegex.length) {
                val char = this@toEndpointRegex[index]
                if (char == '{') {
                    val end = this@toEndpointRegex.indexOf('}', startIndex = index)
                    require(end > index) { "invalid OpenAPI path template ${this@toEndpointRegex}" }
                    append("[^/]+")
                    index = end + 1
                } else {
                    append(Regex.escape(char.toString()))
                    index += 1
                }
            }
        }

    private fun List<String>.requestMethod(): String =
        (optionValue("--method") ?: optionValue("-X") ?: if (fields().isNotEmpty() || optionValue("--input") != null) "POST" else "GET")
            .uppercase()
            .also { method -> require(method == "GET" || method == "POST") { "--method must be GET or POST" } }

    private fun List<String>.requestBody(requestBody: OpenApiRequestBody?): JsonObject? {
        optionValue("--input")?.let { input ->
            val text = Files.readString(Path.of(input))
            return this@ApiCli.json.decodeFromString<JsonObject>(text)
        }
        val fields = fields()
        if (fields.isEmpty()) {
            return null
        }
        val root = linkedMapOf<String, Any?>()
        val schema = requestBody?.content?.get("application/json")?.schema
        fields.forEach { field ->
            val parts = field.key.fieldPath()
            val fieldSchema = schema?.schemaAt(parts)
            val value = if (field.raw) field.value else field.value.toTypedValue(fieldSchema)
            root.putPath(parts, value)
        }
        return root.toJsonObject()
    }

    private fun List<String>.fields(): List<ApiField> =
        buildList {
            optionValues("--field").forEach { add(it.toField(raw = false)) }
            optionValues("-F").forEach { add(it.toField(raw = false)) }
            optionValues("--raw-field").forEach { add(it.toField(raw = true)) }
            optionValues("-f").forEach { add(it.toField(raw = true)) }
        }

    private fun String.toField(raw: Boolean): ApiField {
        val parts = split("=", limit = 2)
        require(parts.size == 2 && parts[0].isNotBlank()) { "field must use key=value syntax" }
        return ApiField(parts[0], parts[1], raw)
    }

    private fun String.fieldPath(): List<String> {
        val root = substringBefore("[")
        require(root.isNotBlank()) { "field name is required" }
        val nested = Regex("""\[([^]]+)]""").findAll(this).map { it.groupValues[1] }.toList()
        return listOf(root) + nested
    }

    private fun String.toTypedValue(schema: OpenApiSchema?): Any? =
        when {
            equals("null", ignoreCase = true) -> null
            equals("true", ignoreCase = true) -> true
            equals("false", ignoreCase = true) -> false
            else -> {
                val enumValues = schema?.enumValues
                when {
                    enumValues != null -> enumValues.matchEnum(this)
                    toIntOrNull() != null -> toInt()
                    toDoubleOrNull() != null -> toDouble()
                    else -> this
                }
            }
        }

    private fun List<String>.matchEnum(value: String): String =
        firstOrNull { candidate ->
            candidate.equals(value, ignoreCase = true) ||
                candidate.equals(value.replace('-', '_'), ignoreCase = true)
        } ?: error("value must be one of ${joinToString(", ")}")

    private fun OpenApiSchema.schemaAt(parts: List<String>): OpenApiSchema? =
        parts.fold(this as OpenApiSchema?) { current, part ->
            current?.properties?.get(part) ?: current?.items?.takeIf { part.toIntOrNull() != null }
        }

    private fun MatchedOperation.help(
        endpoint: String,
        method: String,
    ): String =
        buildString {
            appendLine("Route: $method $path")
            operation.summary?.let { appendLine(it) }
            operation.description?.let { appendLine(it) }
            appendLine("Usage: craftless api $endpoint --method $method [flags]")
            operation.requestBody?.content?.get("application/json")?.schema?.let { schema ->
                appendLine("Fields:")
                schema.describeFields().forEach { field ->
                    appendLine("  $field")
                }
            }
        }.trimEnd()

    private fun OpenApiSchema.describeFields(prefix: String = ""): List<String> =
        properties.flatMap { (name, schema) ->
            val path = if (prefix.isBlank()) name else "$prefix.$name"
            if (schema.type == "object" && schema.properties.isNotEmpty()) {
                schema.describeFields(path)
            } else {
                listOf(schema.fieldSummary(path, name in required))
            }
        }

    private fun OpenApiSchema.fieldSummary(
        path: String,
        required: Boolean,
    ): String =
        buildList {
            add(path)
            add(type)
            if (required) {
                add("required")
            }
            default?.let { add("default=$it") }
            enumValues?.let { add("enum=${it.joinToString("|")}") }
        }.joinToString(" ")

    private fun MutableMap<String, Any?>.putPath(
        parts: List<String>,
        value: Any?,
    ) {
        var current = this
        parts.dropLast(1).forEach { part ->
            @Suppress("UNCHECKED_CAST")
            current = current.getOrPut(part) { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>
        }
        current[parts.last()] = value
    }

    private fun Map<String, Any?>.toJsonObject(): JsonObject =
        buildJsonObject {
            forEach { (key, value) -> put(key, value.toJsonElement()) }
        }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toJsonElement(): JsonElement =
        when (this) {
            null -> JsonPrimitive(null)
            is Boolean -> JsonPrimitive(this)
            is Int -> JsonPrimitive(this)
            is Double -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is Map<*, *> -> (this as Map<String, Any?>).toJsonObject()
            else -> JsonPrimitive(toString())
        }

    private suspend fun io.ktor.client.statement.HttpResponse.forwardBody(
        stdout: (String) -> Unit,
        stderr: (String) -> Unit,
    ): Int {
        val body = bodyAsText()
        return if (status.isSuccess()) {
            stdout(body)
            0
        } else {
            stderr(body)
            1
        }
    }

    private fun List<String>.headers(): List<Pair<String, String>> =
        optionValues("--header").map { it.toHeader() } + optionValues("-H").map { it.toHeader() }

    private fun String.toHeader(): Pair<String, String> {
        val parts = split(":", limit = 2)
        require(parts.size == 2 && parts[0].isNotBlank()) { "header must use name:value syntax" }
        return parts[0].trim() to parts[1].trim()
    }

    private fun List<String>.optionValue(name: String): String? {
        val index = indexOf(name)
        return if (index >= 0 && index + 1 < size) this[index + 1] else null
    }

    private fun List<String>.optionValues(name: String): List<String> =
        mapIndexedNotNull { index, value ->
            if (value == name && index + 1 < size) this[index + 1] else null
        }

    private fun String.pathOnly(): String = substringBefore("?")

    private fun String.clientId(): String? =
        removePrefix("/clients/")
            .takeIf { it != this }
            ?.takeWhile { it != '/' && it != ':' }
            ?.takeIf { it.isNotBlank() }
}

private data class MatchedOperation(
    val method: String,
    val path: String,
    val operation: OpenApiOperation,
)

private data class ApiField(
    val key: String,
    val value: String,
    val raw: Boolean,
)
