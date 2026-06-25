package com.minekube.craftwright.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenApiDocument(
    val openapi: String = "3.1.0",
    val info: OpenApiInfo = OpenApiInfo(),
    val paths: Map<String, OpenApiPath>,
    @SerialName("x-craftwright")
    val extensions: Map<String, String> = emptyMap(),
    @SerialName("x-craftwright-actions")
    val capabilities: List<OpenApiCapability> = emptyList(),
) {
    companion object {
        fun from(
            catalog: ApiRouteCatalog,
            extensions: Map<String, String> = emptyMap(),
            capabilities: List<OpenApiCapability> = emptyList(),
        ): OpenApiDocument {
            val capabilitiesById = capabilities.associateBy { it.id }
            return OpenApiDocument(
                paths = catalog.routes.groupBy { it.path }.mapValues { (_, routes) ->
                    OpenApiPath(
                        get = routes.firstOrNull { it.method == "GET" }?.toOperation(capabilitiesById),
                        post = routes.firstOrNull { it.method == "POST" }?.toOperation(capabilitiesById),
                    )
                },
                extensions = extensions,
                capabilities = capabilities,
            )
        }
    }
}

@Serializable
data class OpenApiInfo(
    val title: String = "Craftwright Client Session API",
    val version: String = "0.1.0",
)

@Serializable
data class OpenApiPath(
    val get: OpenApiOperation? = null,
    val post: OpenApiOperation? = null,
)

@Serializable
data class OpenApiOperation(
    val operationId: String,
    val tags: List<String>,
    val responses: Map<String, OpenApiResponse> = mapOf("200" to OpenApiResponse()),
    val requestBody: OpenApiRequestBody? = null,
    @SerialName("x-craftwright")
    val extensions: Map<String, String>,
)

@Serializable
data class OpenApiResponse(
    val description: String = "OK",
)

@Serializable
data class OpenApiRequestBody(
    val required: Boolean = true,
    val content: Map<String, OpenApiMediaType>,
)

@Serializable
data class OpenApiMediaType(
    val schema: OpenApiSchema,
)

@Serializable
data class OpenApiSchema(
    val type: String,
    val properties: Map<String, OpenApiSchema> = emptyMap(),
    val required: List<String> = emptyList(),
    val additionalProperties: Boolean? = null,
)

@Serializable
data class OpenApiCapability(
    val id: String,
    val schemaVersion: String,
    @SerialName("args")
    val arguments: Map<String, OpenApiCapabilityArgument> = emptyMap(),
)

@Serializable
data class OpenApiCapabilityArgument(
    val type: String,
    val required: Boolean = false,
)

private fun ApiRoute.toOperation(capabilitiesById: Map<String, OpenApiCapability>): OpenApiOperation {
    val route = this
    return OpenApiOperation(
        operationId = operationId,
        tags = listOf(tag),
        requestBody = route.requestBody(capabilitiesById),
        extensions = buildMap {
            put("x-craftwright-java-class", route.javaClass)
            javaMember?.let { put("x-craftwright-java-method", it) }
            put("x-craftwright-thread", thread)
            put("x-craftwright-return", returnKind)
            put("x-craftwright-source", source)
            actionId?.let { put("x-craftwright-action", it) }
        },
    )
}

private fun ApiRoute.requestBody(capabilitiesById: Map<String, OpenApiCapability>): OpenApiRequestBody? =
    when {
        method != "POST" -> null
        actionId != null -> capabilitiesById[actionId]?.arguments?.toRequestBody()
        path.endsWith(":run") -> genericActionRequestBody()
        else -> null
    }

private fun Map<String, OpenApiCapabilityArgument>.toRequestBody(): OpenApiRequestBody =
    OpenApiRequestBody(
        content = mapOf(
            "application/json" to OpenApiMediaType(
                schema = OpenApiSchema(
                    type = "object",
                    properties = mapValues { (_, argument) -> OpenApiSchema(type = argument.type) },
                    required = filterValues { it.required }.keys.toList(),
                )
            )
        )
    )

private fun genericActionRequestBody(): OpenApiRequestBody =
    OpenApiRequestBody(
        content = mapOf(
            "application/json" to OpenApiMediaType(
                schema = OpenApiSchema(
                    type = "object",
                    properties = mapOf(
                        "action" to OpenApiSchema(type = "string"),
                        "args" to OpenApiSchema(type = "object", additionalProperties = true),
                    ),
                    required = listOf("action"),
                )
            )
        )
    )
