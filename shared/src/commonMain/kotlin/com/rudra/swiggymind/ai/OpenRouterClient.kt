package com.rudra.swiggymind.ai

import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.domain.model.RecommendationResponse
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.UserIntent
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

class OpenRouterClient(
    private val apiKey: String,
    private val model: String = AppConstants.OPENROUTER_MODEL
) : LLMClient {

    private val client = createHttpClient()

    override suspend fun parseIntent(
        userMessage: String,
        conversationContext: List<LlmMessage>
    ): UserIntent {
        val systemPrompt = """
            You are the Mind of Swiggy. You are an expert food curator in India who understands nuances of Indian cuisines, budgets, and cravings.
            Your task is to extract structured intent from the user's message.
            Return ONLY valid JSON, no markdown, no explanation.

            Schema:
            {
              "specificCravings": ["string"] (e.g. "biryani", "spicy"),
              "budget": number or null,
              "minBudget": number or null,
              "dietaryPreference": "veg" | "nonveg" | "vegan" | null,
              "mealType": "food" | "grocery" | "dineout" | null,
              "occasion": string or null,
              "mood": string or null,
              "location": string or null,
              "spiceLevel": "mild" | "medium" | "spicy" | null,
              "excludes": ["string"] or null,
              "partySize": number or null
            }
        """.trimIndent()

        return try {
            val response = client.post("${AppConstants.OPENROUTER_BASE_URL}/chat/completions") {
                headers {
                    append("Authorization", "Bearer ${apiKey.trim()}")
                    append("HTTP-Referer", AppConstants.OPENROUTER_REFERER)
                    append("X-Title", AppConstants.OPENROUTER_APP_TITLE)
                    append("Content-Type", "application/json")
                }
                setBody(
                    kotlinx.serialization.json.buildJsonObject {
                        put("model", kotlinx.serialization.json.JsonPrimitive(model))
                        put("max_tokens", kotlinx.serialization.json.JsonPrimitive(AppConstants.MAX_TOKENS))
                        put("temperature", kotlinx.serialization.json.JsonPrimitive(0.7))
                        put("messages", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("role", kotlinx.serialization.json.JsonPrimitive("system"))
                                put("content", kotlinx.serialization.json.JsonPrimitive(systemPrompt))
                            })
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("role", kotlinx.serialization.json.JsonPrimitive("user"))
                                put("content", kotlinx.serialization.json.JsonPrimitive(userMessage))
                            })
                        })
                    }
                )
            }

            if (!response.status.isSuccess()) {
                throw Exception("OpenRouter error: ${response.status}")
            }

            val responseBody = response.body<OpenRouterResponse>()
            val content = responseBody.choices.firstOrNull()?.message?.content ?: ""
            AiJsonParser.decodeOrNull<UserIntent>(content)?.copy(rawQuery = userMessage)
                ?: UserIntent(rawQuery = userMessage)
        } catch (e: Exception) {
            UserIntent(rawQuery = userMessage)
        }
    }

    override suspend fun generateRecommendation(
        intent: UserIntent,
        candidates: List<Restaurant>,
        conversationContext: List<LlmMessage>
    ): RecommendationResponse? {
        val raw = generateRecommendationRaw(intent, candidates, conversationContext) ?: return null
        return AiJsonParser.decodeOrNull<RecommendationResponse>(raw)
    }

    override suspend fun generateGeneric(prompt: String, systemPrompt: String?): String? {
        return try {
            val response = client.post("${AppConstants.OPENROUTER_BASE_URL}/chat/completions") {
                headers {
                    append("Authorization", "Bearer ${apiKey.trim()}")
                    append("HTTP-Referer", AppConstants.OPENROUTER_REFERER)
                    append("X-Title", AppConstants.OPENROUTER_APP_TITLE)
                    append("Content-Type", "application/json")
                }
                setBody(
                    kotlinx.serialization.json.buildJsonObject {
                        put("model", kotlinx.serialization.json.JsonPrimitive(model))
                        put("max_tokens", kotlinx.serialization.json.JsonPrimitive(AppConstants.MAX_TOKENS))
                        put("temperature", kotlinx.serialization.json.JsonPrimitive(0.7))
                        put("messages", kotlinx.serialization.json.buildJsonArray {
                            if (systemPrompt != null) {
                                add(kotlinx.serialization.json.buildJsonObject {
                                    put("role", kotlinx.serialization.json.JsonPrimitive("system"))
                                    put("content", kotlinx.serialization.json.JsonPrimitive(systemPrompt))
                                })
                            }
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("role", kotlinx.serialization.json.JsonPrimitive("user"))
                                put("content", kotlinx.serialization.json.JsonPrimitive(prompt))
                            })
                        })
                    }
                )
            }

            if (!response.status.isSuccess()) {
                throw Exception("OpenRouter error: ${response.status}")
            }

            val responseBody = response.body<OpenRouterResponse>()
            responseBody.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateRecommendationRaw(
        intent: UserIntent,
        candidates: List<Restaurant>,
        conversationContext: List<LlmMessage>
    ): String? {
        val restaurantsList = candidates.joinToString("\n") {
            "ID: ${it.id}, Name: ${it.name}, Cuisine: ${it.cuisine.joinToString()}, Rating: ${it.rating}, CostForTwo: ${it.costForTwo}, Tags: ${it.tags.joinToString()}"
        }

        val systemPrompt = """
            You are SwiggyMind — the expert food curator at Swiggy. 
            You know every restaurant, every dish, and exactly what the user is looking for.
            Pick the top 3 best candidates and explain in 1 warm, helpful sentence why they match.
            Be conversational and specific. Avoid saying 'As an AI' or 'I recommend'. 
            Just be the helpful friend who knows the best food spots.
            Return ONLY valid JSON:
            {
              "picks": [
                {
                  "restaurantId": "string",
                  "reason": "string"
                }
              ],
              "summary": "string (one line overall response to user)"
            }
        """.trimIndent()

        val userPrompt = "User intent: $intent\nCandidates:\n$restaurantsList"

        return try {
            val response = client.post("${AppConstants.OPENROUTER_BASE_URL}/chat/completions") {
                headers {
                    append("Authorization", "Bearer ${apiKey.trim()}")
                    append("HTTP-Referer", AppConstants.OPENROUTER_REFERER)
                    append("X-Title", AppConstants.OPENROUTER_APP_TITLE)
                    append("Content-Type", "application/json")
                }
                setBody(
                    kotlinx.serialization.json.buildJsonObject {
                        put("model", kotlinx.serialization.json.JsonPrimitive(model))
                        put("max_tokens", kotlinx.serialization.json.JsonPrimitive(AppConstants.MAX_TOKENS))
                        put("temperature", kotlinx.serialization.json.JsonPrimitive(0.7))
                        put("messages", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("role", kotlinx.serialization.json.JsonPrimitive("system"))
                                put("content", kotlinx.serialization.json.JsonPrimitive(systemPrompt))
                            })
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("role", kotlinx.serialization.json.JsonPrimitive("user"))
                                put("content", kotlinx.serialization.json.JsonPrimitive(userPrompt))
                            })
                        })
                    }
                )
            }

            if (!response.status.isSuccess()) {
                throw Exception("OpenRouter error: ${response.status}")
            }

            val rawBody: String = response.bodyAsText()
            val parsedResponse = AiJsonParser.json.decodeFromString<OpenRouterResponse>(rawBody)
            if (parsedResponse.error != null) {
                return null
            }
            parsedResponse.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    data class OpenRouterRequest(
        val model: String,
        val messages: List<Message>,
        val response_format: JsonElement? = null
    )

    @Serializable
    data class Message(val role: String, val content: String)

    @Serializable
    data class OpenRouterResponse(
        val choices: List<Choice> = emptyList(),
        val error: OpenRouterError? = null
    )

    @Serializable
    data class OpenRouterError(val message: String = "", val code: Int? = null)

    @Serializable
    data class Choice(val message: Message)
}
