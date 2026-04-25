package com.rudra.swiggymind.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object AiSchemas {
    val userIntentResponseFormat = JsonObject(
        mapOf(
            "type" to JsonPrimitive("json_schema"),
            "json_schema" to JsonObject(
                mapOf(
                    "name" to JsonPrimitive("user_intent"),
                    "strict" to JsonPrimitive(true),
                    "schema" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "properties" to JsonObject(
                                mapOf(
                                    "specificCravings" to stringArrayProperty(),
                                    "budget" to nullableNumberProperty(),
                                    "minBudget" to nullableNumberProperty(),
                                    "dietaryPreference" to nullableStringProperty(),
                                    "mealType" to nullableStringProperty(),
                                    "occasion" to nullableStringProperty(),
                                    "mood" to nullableStringProperty(),
                                    "location" to nullableStringProperty(),
                                    "spiceLevel" to nullableStringProperty(),
                                    "excludes" to stringArrayProperty(),
                                    "partySize" to nullableNumberProperty()
                                )
                            ),
                            "required" to JsonArray(
                                listOf(
                                    JsonPrimitive("specificCravings"),
                                    JsonPrimitive("budget"),
                                    JsonPrimitive("minBudget"),
                                    JsonPrimitive("dietaryPreference"),
                                    JsonPrimitive("mealType"),
                                    JsonPrimitive("occasion"),
                                    JsonPrimitive("mood"),
                                    JsonPrimitive("location"),
                                    JsonPrimitive("spiceLevel"),
                                    JsonPrimitive("excludes"),
                                    JsonPrimitive("partySize")
                                )
                            ),
                            "additionalProperties" to JsonPrimitive(false)
                        )
                    )
                )
            )
        )
    )

    val recommendationResponseFormat = JsonObject(
        mapOf(
            "type" to JsonPrimitive("json_schema"),
            "json_schema" to JsonObject(
                mapOf(
                    "name" to JsonPrimitive("restaurant_recommendations"),
                    "strict" to JsonPrimitive(true),
                    "schema" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "properties" to JsonObject(
                                mapOf(
                                    "summary" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                                    "picks" to JsonObject(
                                        mapOf(
                                            "type" to JsonPrimitive("array"),
                                            "items" to JsonObject(
                                                mapOf(
                                                    "type" to JsonPrimitive("object"),
                                                    "properties" to JsonObject(
                                                        mapOf(
                                                            "restaurantId" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                                                            "reason" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                                                        )
                                                    ),
                                                    "required" to JsonArray(listOf(JsonPrimitive("restaurantId"), JsonPrimitive("reason"))),
                                                    "additionalProperties" to JsonPrimitive(false)
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            "required" to JsonArray(listOf(JsonPrimitive("summary"), JsonPrimitive("picks"))),
                            "additionalProperties" to JsonPrimitive(false)
                        )
                    )
                )
            )
        )
    )

    private fun stringArrayProperty(): JsonObject {
        return JsonObject(
            mapOf(
                "type" to JsonPrimitive("array"),
                "items" to JsonObject(mapOf("type" to JsonPrimitive("string")))
            )
        )
    }

    private fun nullableStringProperty(): JsonObject {
        return JsonObject(
            mapOf(
                "type" to JsonArray(listOf(JsonPrimitive("string"), JsonPrimitive("null")))
            )
        )
    }

    private fun nullableNumberProperty(): JsonObject {
        return JsonObject(
            mapOf(
                "type" to JsonArray(listOf(JsonPrimitive("number"), JsonPrimitive("null")))
            )
        )
    }
}
