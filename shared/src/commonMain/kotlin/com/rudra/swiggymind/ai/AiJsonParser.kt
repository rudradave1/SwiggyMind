package com.rudra.swiggymind.ai

import kotlinx.serialization.json.Json

object AiJsonParser {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun clean(raw: String): String {
        return raw
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()
    }

    fun extractJsonBlock(raw: String): String? {
        val cleaned = clean(raw)
        val objectStart = cleaned.indexOf('{')
        val arrayStart = cleaned.indexOf('[')
        val start = listOf(objectStart, arrayStart)
            .filter { it >= 0 }
            .minOrNull() ?: return null

        val endChar = if (cleaned[start] == '[') ']' else '}'
        val end = cleaned.lastIndexOf(endChar)
        if (end < start) return null

        return cleaned.substring(start, end + 1)
    }

    inline fun <reified T> decodeOrNull(raw: String): T? {
        val jsonBlock = extractJsonBlock(raw) ?: return null
        return runCatching {
            json.decodeFromString<T>(jsonBlock)
        }.getOrNull()
    }
}
