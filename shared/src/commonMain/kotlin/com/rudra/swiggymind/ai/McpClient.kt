package com.rudra.swiggymind.ai

import com.rudra.swiggymind.AppConstants
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Minimal MCP JSON-RPC client.
 *
 * Protocol: POST {serverUrl} with { jsonrpc, method: "tools/call", params: { name, arguments } }
 * Response: { result: { content: [{ type: "text", text: "{success,data}" }] } }
 *
 * For local stub: base = "http://10.0.2.2:3000"
 * For staging:    base = "https://mcp.swiggy.com"
 */
class McpClient(
    private val accessToken: String,
    private val baseUrl: String = AppConstants.MCP_BASE_URL_LOCAL
) {
    private val httpClient = createHttpClient()
    private var callCounter = 0

    suspend fun callTool(
        serverPath: String,
        toolName: String,
        arguments: JsonObject = buildJsonObject {}
    ): String? {
        val result = makeRequest(baseUrl, serverPath, toolName, arguments)
        if (result != null) return result
        
        // If baseUrl is 10.0.2.2 and failed, try localhost (for different environments)
        if (baseUrl == AppConstants.MCP_BASE_URL_LOCAL) {
            return makeRequest("http://localhost:3000", serverPath, toolName, arguments)
        }
        
        return null
    }

    private suspend fun makeRequest(
        base: String,
        serverPath: String,
        toolName: String,
        arguments: JsonObject
    ): String? = runCatching {
        val response = httpClient.post("$base$serverPath") {
            contentType(ContentType.Application.Json)
            if (accessToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            setBody(buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "tools/call")
                put("id", "${++callCounter}")
                put("params", buildJsonObject {
                    put("name", toolName)
                    put("arguments", arguments)
                })
            })
        }

        if (!response.status.isSuccess()) return@runCatching null

        val mcpResponse = response.body<McpResponse>()
        mcpResponse.result?.content?.firstOrNull { it.type == "text" }?.text
    }.getOrNull()

    // ── MCP wire models ───────────────────────────────────────────────────────

    @Serializable
    data class McpResponse(
        val jsonrpc: String = "2.0",
        val id: String? = null,
        val result: McpResult? = null,
        val error: McpError? = null
    )

    @Serializable
    data class McpResult(val content: List<McpContent> = emptyList())

    @Serializable
    data class McpContent(val type: String, val text: String = "")

    @Serializable
    data class McpError(val code: Int = 0, val message: String = "")
}
