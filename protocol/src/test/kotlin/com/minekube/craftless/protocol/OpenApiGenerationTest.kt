package com.minekube.craftless.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiGenerationTest {
    @Test
    fun `openapi document includes craftless metadata for generic action route`() {
        val document = OpenApiDocument.from(ApiRouteCatalog.sessionDefaults())

        val operation = document.paths["/clients/{id}:run"]?.post
        assertNotNull(operation)
        assertEquals("runClientAction", operation.operationId)
        assertEquals("clients", operation.tags.single())
        assertTrue(operation.extensions.keys.none { it.startsWith("x-craftless-java-") })
        assertTrue(operation.extensions.keys.none { it == "x-craftless-thread" })
        assertEquals("clients", operation.extensions["x-craftless-owner"])
        assertEquals("client", operation.extensions["x-craftless-target"])
        assertEquals("action", operation.extensions["x-craftless-source"])
        assertEquals("run", operation.extensions["x-craftless-member"])
        val schema = operation.requestBody?.content?.get("application/json")?.schema
        assertNotNull(schema)
        assertEquals("object", schema.type)
        assertEquals(listOf("action"), schema.required)
        assertEquals("string", schema.properties["action"]?.type)
        assertEquals("object", schema.properties["args"]?.type)
        val responseSchema = operation.responses["200"]
            ?.content
            ?.get("application/json")
            ?.schema
        assertNotNull(responseSchema)
        assertEquals("object", responseSchema.type)
        assertEquals(listOf("action", "status"), responseSchema.required)
        assertEquals("string", responseSchema.properties["action"]?.type)
        assertEquals("string", responseSchema.properties["status"]?.type)
        assertEquals("string", responseSchema.properties["message"]?.type)
    }

    @Test
    fun `stable lifecycle routes describe create and connect request bodies`() {
        val document = OpenApiDocument.from(ApiRouteCatalog.sessionDefaults())
        val versionOperation = document.paths["/version"]?.get
        assertNotNull(versionOperation)
        assertEquals("supervisor", versionOperation.extensions["x-craftless-target"])

        val createSchema = document.paths["/clients"]?.post
            ?.requestBody
            ?.content
            ?.get("application/json")
            ?.schema
        assertNotNull(createSchema)
        assertEquals("object", createSchema.type)
        assertEquals(listOf("id", "version", "loader", "profile"), createSchema.required)
        assertEquals("string", createSchema.properties["id"]?.type)
        assertEquals("string", createSchema.properties["version"]?.type)
        assertEquals("string", createSchema.properties["loader"]?.type)
        val profileSchema = createSchema.properties["profile"]
        assertNotNull(profileSchema)
        assertEquals("object", profileSchema.type)
        assertEquals(listOf("kind", "name"), profileSchema.required)
        assertEquals("string", profileSchema.properties["kind"]?.type)
        assertEquals("string", profileSchema.properties["name"]?.type)

        val connectSchema = document.paths["/clients/{id}:connect"]?.post
            ?.requestBody
            ?.content
            ?.get("application/json")
            ?.schema
        assertNotNull(connectSchema)
        assertEquals("object", connectSchema.type)
        assertEquals(listOf("host", "port"), connectSchema.required)
        assertEquals("string", connectSchema.properties["host"]?.type)
        assertEquals("integer", connectSchema.properties["port"]?.type)
    }

    @Test
    fun `stable lifecycle routes describe client response bodies`() {
        val document = OpenApiDocument.from(ApiRouteCatalog.sessionDefaults())

        val listSchema = document.paths["/clients"]?.get?.okSchema()
        assertNotNull(listSchema)
        assertEquals("array", listSchema.type)
        val listItemSchema = listSchema.items
        assertNotNull(listItemSchema)
        assertClientSchema(listItemSchema)

        assertClientSchema(requireNotNull(document.paths["/clients"]?.post?.successSchema("201")))
        assertClientSchema(requireNotNull(document.paths["/clients/{id}"]?.get?.okSchema()))
        assertClientSchema(requireNotNull(document.paths["/clients/{id}:connect"]?.post?.okSchema()))
        assertClientSchema(requireNotNull(document.paths["/clients/{id}:stop"]?.post?.okSchema()))
    }

    @Test
    fun `stable routes describe machine readable error responses`() {
        val document = OpenApiDocument.from(ApiRouteCatalog.sessionDefaults())

        assertErrorSchema(requireNotNull(document.paths["/clients"]?.post?.errorSchema("400")))
        assertErrorSchema(requireNotNull(document.paths["/clients/{id}"]?.get?.errorSchema("404")))
        assertErrorSchema(requireNotNull(document.paths["/clients/{id}/actions"]?.get?.errorSchema("404")))
        assertErrorSchema(requireNotNull(document.paths["/clients/{id}:connect"]?.post?.errorSchema("400")))
        assertErrorSchema(requireNotNull(document.paths["/clients/{id}:run"]?.post?.errorSchema("400")))
        assertErrorSchema(requireNotNull(document.paths["/clients/{id}:run"]?.post?.errorSchema("404")))
        assertErrorSchema(requireNotNull(document.paths["/clients/{id}:stop"]?.post?.errorSchema("404")))
    }

    private fun OpenApiOperation.okSchema(): OpenApiSchema? =
        successSchema("200")

    private fun OpenApiOperation.successSchema(status: String): OpenApiSchema? =
        responses[status]?.content?.get("application/json")?.schema

    private fun OpenApiOperation.errorSchema(status: String): OpenApiSchema? =
        responses[status]?.content?.get("application/json")?.schema

    private fun assertErrorSchema(schema: OpenApiSchema) {
        assertEquals("object", schema.type)
        assertEquals(listOf("code", "message"), schema.required)
        assertEquals("string", schema.properties["code"]?.type)
        assertEquals("string", schema.properties["message"]?.type)
    }

    private fun assertClientSchema(schema: OpenApiSchema) {
        assertEquals("object", schema.type)
        assertEquals(listOf("id", "instance", "profile", "state"), schema.required)
        assertEquals("string", schema.properties["id"]?.type)
        assertEquals("string", schema.properties["state"]?.type)

        val instanceSchema = schema.properties["instance"]
        assertNotNull(instanceSchema)
        assertEquals("object", instanceSchema.type)
        assertEquals(listOf("id", "version", "loader"), instanceSchema.required)
        assertEquals("string", instanceSchema.properties["id"]?.type)
        assertEquals("string", instanceSchema.properties["loader"]?.type)
        val versionSchema = instanceSchema.properties["version"]
        assertNotNull(versionSchema)
        assertEquals("object", versionSchema.type)
        assertEquals(listOf("id"), versionSchema.required)
        assertEquals("string", versionSchema.properties["id"]?.type)

        val profileSchema = schema.properties["profile"]
        assertNotNull(profileSchema)
        assertEquals("object", profileSchema.type)
        assertEquals(listOf("kind", "name"), profileSchema.required)
        assertEquals("string", profileSchema.properties["kind"]?.type)
        assertEquals("string", profileSchema.properties["name"]?.type)
    }
}
