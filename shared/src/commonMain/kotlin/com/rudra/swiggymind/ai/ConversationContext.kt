package com.rudra.swiggymind.ai

data class LlmMessage(
    val role: String,
    val content: String
)

object ConversationContext {
    private const val MAX_RECENT_MESSAGES = 6

    fun trim(messages: List<LlmMessage>): List<LlmMessage> {
        if (messages.isEmpty()) return emptyList()

        val recentMessages = messages.takeLast(MAX_RECENT_MESSAGES)
        val olderMessages = messages.dropLast(recentMessages.size)
        if (olderMessages.isEmpty()) return recentMessages

        val summary = buildSummary(olderMessages)
        return listOf(LlmMessage(role = "system", content = summary)) + recentMessages
    }

    private fun buildSummary(messages: List<LlmMessage>): String {
        val text = messages.joinToString(" ") { it.content.lowercase() }

        val preferences = buildList {
            when {
                "vegan" in text -> add("user prefers vegan food")
                "non veg" in text || "non-veg" in text || "chicken" in text -> add("user is open to non-veg options")
                "veg" in text -> add("user prefers veg food")
            }

            Regex("""(?:under|below|within)\s*[₹rs\. ]*\s*(\d{2,5})""")
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { add("budget under Rs$it") }

            when {
                "spicy" in text -> add("likes spicy food")
                "mild" in text -> add("prefers mild flavors")
            }

            if ("grocery" in text || "instamart" in text) add("may want grocery suggestions")
            if ("dineout" in text || "table" in text) add("may want dineout options")
        }

        return if (preferences.isEmpty()) {
            "Previous context: ${messages.takeLast(3).joinToString(" | ") { "${it.role}: ${it.content.take(80)}" }}"
        } else {
            "Previous context: ${preferences.joinToString(", ")}."
        }
    }
}
