package com.rudra.swiggymind.ai

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

class AiConnectivityChecker {
    private val client = createHttpClient()

    suspend fun checkOllama(baseUrl: String): String {
        val normalizedUrl = baseUrl.trim().removeSuffix("/").let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it"
        }

        return try {
            val response: OllamaTagsResponse = client.get("$normalizedUrl/api/tags").body()
            val modelNames = response.models.map { it.name }
            if (modelNames.isEmpty()) {
                "Ollama is reachable, but no local models are installed. Pull one like `ollama pull llama3.1:8b`."
            } else {
                "Ollama connected. Found models: ${modelNames.joinToString()}."
            }
        } catch (e: Exception) {
            "Ollama check failed: ${e.message ?: e::class.simpleName.orEmpty()}"
        }
    }

    suspend fun checkOpenRouter(apiKey: String): String {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return "OpenRouter check failed: API key is empty."
        }

        return try {
            val response: OpenRouterKeyResponse = client.get("https://openrouter.ai/api/v1/key") {
                headers.append(HttpHeaders.Authorization, "Bearer $trimmedKey")
                headers.append("HTTP-Referer", "https://swiggymind.local")
                headers.append("X-OpenRouter-Title", "SwiggyMind")
            }.body()

            val label = response.data.label ?: "Unnamed key"
            val freeTier = response.data.isFreeTier?.let { if (it) "free-tier" else "paid/byok" } ?: "unknown-tier"
            "OpenRouter connected. Key: $label, tier: $freeTier."
        } catch (e: Exception) {
            "OpenRouter check failed: ${e.message ?: e::class.simpleName.orEmpty()}"
        }
    }
}

@Serializable
private data class OllamaTagsResponse(
    val models: List<OllamaModel> = emptyList()
)

@Serializable
private data class OllamaModel(
    val name: String
)

@Serializable
private data class OpenRouterKeyResponse(
    val data: OpenRouterKeyData
)

@Serializable
private data class OpenRouterKeyData(
    val label: String? = null,
    val isFreeTier: Boolean? = null
)
