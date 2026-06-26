package com.minekube.craftless.driver.api

import com.minekube.craftless.protocol.RuntimeOperationNode
import kotlinx.serialization.json.JsonElement

fun interface DriverOperationAdapter {
    fun invoke(invocation: DriverOperationInvocation): DriverActionResult
}

data class DriverOperationInvocation(
    val clientId: String,
    val operation: RuntimeOperationNode,
    val arguments: Map<String, JsonElement> = emptyMap(),
)

class DriverOperationAdapters(
    adapters: Map<String, DriverOperationAdapter>,
) {
    private val adapters = adapters.toSortedMap()

    init {
        require(this.adapters.keys.all { it.isNotBlank() }) { "operation adapter key is required" }
    }

    fun invoke(invocation: DriverOperationInvocation): DriverActionResult {
        val adapter =
            adapters[invocation.operation.adapter]
                ?: throw IllegalArgumentException("operation adapter ${invocation.operation.adapter} is not registered")
        return adapter.invoke(invocation)
    }

    fun adapterKeys(): Set<String> = adapters.keys

    companion object {
        fun empty(): DriverOperationAdapters = DriverOperationAdapters(emptyMap())
    }
}
