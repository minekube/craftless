package com.minekube.craftless.protocol

import kotlinx.serialization.Serializable

@Serializable
data class ApiRoute(
    val method: String,
    val path: String,
    val operationId: String,
    val tag: String,
    val javaClass: String,
    val javaMember: String? = null,
    val thread: String = "client",
    val source: String,
    val returnKind: String = "value",
    val actionId: String? = null,
)

class ApiRouteCatalog(
    val routes: List<ApiRoute>,
) {
    private val byPath: Map<String, ApiRoute> = routes.associateBy { it.path }
    private val byMethodAndPath: Map<Pair<String, String>, ApiRoute> =
        routes.associateBy { it.method to it.path }

    fun route(path: String): ApiRoute =
        byPath[path] ?: error("route not found: $path")

    fun route(method: String, path: String): ApiRoute =
        byMethodAndPath[method to path] ?: error("route not found: $method $path")

    companion object {
        fun sessionDefaults(): ApiRouteCatalog = ApiRouteCatalog(
            listOf(
                route("GET", "/openapi.json", "getOpenapiJson", "openapi", "com.minekube.craftless.openapi", "openapi", "route"),
                route("GET", "/version", "getVersion", "version", "com.minekube.craftless.version", "version", "route"),
                route("GET", "/events", "getEvents", "events", "com.minekube.craftless.events", "events", "route"),
                route("GET", "/clients", "listClients", "clients", "com.minekube.craftless.daemon.clients", "list", "route"),
                route("POST", "/clients", "createClient", "clients", "com.minekube.craftless.daemon.clients", "create", "route"),
                route("GET", "/clients/{id}", "getClient", "clients", "com.minekube.craftless.daemon.clients", "get", "route"),
                route("GET", "/clients/{id}/openapi.json", "getClientOpenapiJson", "clients", "com.minekube.craftless.daemon.clients", "openapi", "route"),
                route("POST", "/clients/{id}:connect", "clientConnect", "clients", "com.minekube.craftless.daemon.clients", "connect", "method"),
                route("GET", "/clients/{id}/actions", "listClientActions", "clients", "com.minekube.craftless.daemon.clients", "actions", "action"),
                route("POST", "/clients/{id}:run", "runClientAction", "clients", "com.minekube.craftless.daemon.clients", "run", "action"),
                route("POST", "/clients/{id}:stop", "stopClient", "clients", "com.minekube.craftless.daemon.clients", "stop", "method"),
                route("GET", "/clients/{id}/events", "getClientEvents", "clients", "com.minekube.craftless.daemon.clients", "events", "route"),
            )
        )

        private fun route(
            method: String,
            path: String,
            operationId: String,
            tag: String,
            javaClass: String,
            javaMember: String,
            source: String,
            returnKind: String = "value",
        ): ApiRoute = ApiRoute(
            method = method,
            path = path,
            operationId = operationId,
            tag = tag,
            javaClass = javaClass,
            javaMember = javaMember,
            source = source,
            returnKind = returnKind,
        )
    }
}
