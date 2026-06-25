package dev.minekube.craftwright.driver.fabric.v1_21_6

import dev.minekube.craftwright.driver.api.ChatCommand
import dev.minekube.craftwright.driver.api.ConnectionTarget
import dev.minekube.craftwright.driver.runtime.DriverBackendAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FabricDriverModuleTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fabric metadata declares client entrypoint and mixin config`() {
        val metadata = resourceJson("fabric.mod.json")

        assertEquals("craftwright-driver-fabric-1-21-6", metadata["id"]?.jsonPrimitive?.content)
        assertEquals("0.1.0-SNAPSHOT", metadata["version"]?.jsonPrimitive?.content)
        assertEquals("dev.minekube.craftwright.driver.fabric.v1_21_6.CraftwrightFabricClientEntrypoint", metadata["entrypoints"]
            ?.jsonObject
            ?.get("client")
            ?.jsonArray
            ?.single()
            ?.jsonPrimitive
            ?.content)
        assertEquals("craftwright-driver-fabric-1_21_6.mixins.json", metadata["mixins"]?.jsonArray?.single()?.jsonPrimitive?.content)

        val mixins = resourceJson("craftwright-driver-fabric-1_21_6.mixins.json")
        assertEquals("dev.minekube.craftwright.driver.fabric.v1_21_6.mixin", mixins["package"]?.jsonPrimitive?.content)
        assertEquals("client", mixins["environment"]?.jsonPrimitive?.content)
    }

    @Test
    fun `fabric backend exposes driver runtime actions without changing daemon contract`() {
        val backend = FabricDriverBackend.placeholder()

        assertEquals(
            DriverBackendAction.CONNECT,
            backend.connect("alice", ConnectionTarget("127.0.0.1", 25565)).action,
        )
        assertEquals(
            DriverBackendAction.CHAT,
            backend.sendChat("alice", ChatCommand("hello fabric")).action,
        )
        assertEquals(DriverBackendAction.STOP, backend.stop("alice").action)
        assertTrue(backend.events().any { it.contains("connect alice 127.0.0.1:25565") })
    }

    private fun resourceJson(path: String) =
        json.parseToJsonElement(
            requireNotNull(javaClass.classLoader.getResource(path)) { "missing resource $path" }.readText()
        ).jsonObject
}
