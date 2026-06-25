package com.minekube.craftwright.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpenApiGenerationTest {
    @Test
    fun `openapi document includes craftwright metadata for generic action route`() {
        val document = OpenApiDocument.from(ApiRouteCatalog.sessionDefaults())

        val operation = document.paths["/clients/{id}:run"]?.post
        assertNotNull(operation)
        assertEquals("runClientAction", operation.operationId)
        assertEquals("clients", operation.tags.single())
        assertEquals("com.minekube.craftwright.daemon.clients", operation.extensions["x-craftwright-java-class"])
        assertEquals("run", operation.extensions["x-craftwright-java-method"])
        assertEquals("client", operation.extensions["x-craftwright-thread"])
        assertEquals("action", operation.extensions["x-craftwright-source"])
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

        val connectSchema = document.paths["/clients/{id}/connection/connect"]?.post
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
}
