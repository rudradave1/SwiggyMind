# SwiggyMind 🧠

> An AI ordering copilot that understands what you're craving — not just what you type.

<p align="center">
  <img src="https://github.com/user-attachments/assets/51aa3505-83f1-4c7b-a480-186e996bdfe3" width="280" alt="SwiggyMind Demo"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/KMP-Multiplatform-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/AI-OpenRouter-FC8019?style=flat-square"/>
  <img src="https://img.shields.io/badge/Built%20for-Swiggy%20Builders%20Club-FC8019?style=flat-square"/>
  <img src="https://img.shields.io/github/license/rudradave1/SwiggyMind?style=flat-square"/>
</p>

---

## The Problem

Swiggy today requires you to already know what you want. You browse, filter manually, scroll endlessly. SwiggyMind flips this entirely.

Tell it what you're feeling. It reasons its way to a recommendation — with an explanation.

```
"Something light, not too oily, under ₹180"        →  3 ranked picks with reasoning
"Grocery list for biryani for 4 people"             →  Parsed ingredient list + Instamart link
"Book a table for two this evening, rooftop"        →  Dineout recommendations with context
```

---

## What Makes It Different

| Swiggy Today | SwiggyMind |
|---|---|
| Browse by cuisine or restaurant | Describe your craving in natural language |
| Manual filters for price, diet, time | Intent parsed automatically |
| See a list, decide yourself | Ranked picks with AI reasoning |
| No memory of your preferences | Builds your Food DNA over time |

---

## Architecture

```mermaid
graph TD
    A[User Message] --> B[Chat UI\nJetpack Compose]
    B --> C[ChatViewModel\nCoroutines + StateFlow]
    C --> D[AssistantUseCases]
    D --> E[ResponseOrchestrator]

    E --> M[HeuristicIntentParser\nDetects food · grocery · dineout]

    M -->|grocery| GR[getInstamartItems\nIngredient extraction\n+ Instamart link]
    M -->|dineout| DR[getDineoutVenues\nVenue recommendations]
    M -->|food| FR[getRestaurants\nFiltered by intent]

    FR --> P[providerAttempts]
    P -->|Layer 1 — Primary| F[OpenRouterClient\nmeta-llama/llama-3.1-8b-instruct:free]
    P -->|Layer 2 — Recovery| G[AiJsonParser\nrecover from raw LLM text]
    P -->|Layer 3 — Fallback| H[buildRuleBasedFallback\ngenerateWhyMatch per result]
    P -->|Layer 4 — Last Resort| I[buildRelaxedFallback\nrelax filters, top by rating]

    F --> O[OrchestratedResponse]
    G --> O
    H --> O
    I --> O
    GR --> O
    DR --> O

    O --> Q[Restaurant Cards\nwith AI reasoning]
    O --> R[AppDatabase\nChatHistory — Room]
    R --> S[Food DNA\nTaste profile from history]

    style E fill:#FFF3E8,stroke:#FC8019
    style F fill:#E8F5E9,stroke:#2E7D32
    style P fill:#FFF3E8,stroke:#FC8019
```

---

## AI Layer — 4-Layer Response Guarantee

SwiggyMind **never** shows an error message. `ResponseOrchestrator` ensures every query returns a useful result.

```
┌─────────────────────────────────────────────────────┐
│  Layer 1 — OpenRouterClient (Primary)               │
│  meta-llama/llama-3.1-8b-instruct:free              │
│  Timeout: 8 seconds via withTimeoutOrNull           │
│  Badge shown: 🟠 AI-Powered                         │
├─────────────────────────────────────────────────────┤
│  Layer 2 — AiJsonParser Recovery                    │
│  LLM responded but JSON malformed                   │
│  recoverFromRawResponses — ID match, name match,    │
│  fuzzy token scoring against restaurant pool        │
│  Badge shown: 🟠 AI-Powered                         │
├─────────────────────────────────────────────────────┤
│  Layer 3 — buildRuleBasedFallback                   │
│  No LLM available                                   │
│  HeuristicIntentParser filters → sort by rating     │
│  generateWhyMatch builds specific reason per card   │
│  Badge shown: ⚪ Top Rated                          │
├─────────────────────────────────────────────────────┤
│  Layer 4 — buildRelaxedFallback                     │
│  Filters returned 0 results                         │
│  Relaxes constraints, top 3 by rating from all      │
│  Badge shown: ⚪ Top Rated                          │
└─────────────────────────────────────────────────────┘
```

The AI is a **progressive enhancement**, not a dependency. The app works fully offline via `HeuristicIntentParser`.

---

## Tech Stack

```
┌─────────────────────────────────────────────────────┐
│  Presentation                                       │
│  Jetpack Compose · Material 3 · Plus Jakarta Sans  │
│  Animated transitions · Coil image loading          │
├─────────────────────────────────────────────────────┤
│  Architecture                                       │
│  Clean Architecture · MVVM · KMP shared module      │
│  Kotlin Coroutines · StateFlow · Result<T>          │
├─────────────────────────────────────────────────────┤
│  Shared Module (commonMain)                         │
│  ResponseOrchestrator — 4-layer fallback chain      │
│  HeuristicIntentParser — local intent parsing       │
│  AiSchemas — structured intent models               │
│  AiJsonParser — LLM response recovery               │
│  ConversationContext — multi-turn context mgmt      │
│  AiConnectivityChecker — live status detection      │
├─────────────────────────────────────────────────────┤
│  Data                                               │
│  AppDatabase (Room) · ChatHistory entities          │
│  RestaurantRepository · SettingsRepository          │
│  OpenRouterClient (Ktor)                            │
│  kotlinx.serialization                              │
├─────────────────────────────────────────────────────┤
│  DI · Build                                         │
│  Hilt · SharedComponent · Gradle version catalogs  │
│  GitHub Actions CI                                  │
└─────────────────────────────────────────────────────┘
```

---

## Shared Module Structure

```
shared/src/commonMain/kotlin/com/rudra/swiggymind/
│
├── ai/
│   ├── AiConnectivityChecker.kt   # Live OpenRouter status detection
│   ├── AiJsonParser.kt            # LLM response recovery + parsing
│   ├── AiSchemas.kt               # Structured intent models
│   ├── ConversationContext.kt     # Multi-turn context trimming
│   ├── HttpClientFactory.kt       # Ktor client setup
│   ├── LLMClient.kt               # LLM interface
│   └── OpenRouterClient.kt        # OpenRouter implementation
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room database
│   │   └── ChatHistory.kt         # Conversation persistence
│   └── repository/
│       └── RestaurantRepository.kt # Interface + mock impl
│                                   # ← all 3 MCP integration points
│
├── domain/
│   ├── repository/
│   │   └── SettingsRepository.kt
│   └── usecase/
│       ├── AssistantUseCases.kt
│       ├── HeuristicIntentParser.kt
│       └── ResponseOrchestrator.kt
│
└── AppConstants.kt
```

---

## The MCP Integration Points

`RestaurantRepository` is the single boundary between SwiggyMind and Swiggy's data. It already exposes exactly the three methods Swiggy's MCP servers map to:

```kotlin
interface RestaurantRepository {
    // → Swiggy Food MCP
    suspend fun getRestaurants(intent: UserIntent? = null): List<Restaurant>

    // → Swiggy Instamart MCP
    suspend fun getInstamartItems(intent: UserIntent? = null): List<InstamartItem>

    // → Swiggy Dineout MCP
    suspend fun getDineoutVenues(intent: UserIntent? = null): List<Restaurant>

    suspend fun getRestaurantById(id: String): Restaurant?
}
```

Today each method returns filtered mock JSON. With Builders Club API access, each method becomes a live MCP call — the `ResponseOrchestrator`, `HeuristicIntentParser`, `AiJsonParser`, and all four fallback layers continue working identically. No other files change.

---

## Features

**Conversational Discovery**
Natural language parsed into structured intent — cuisine, budget, dietary preference, spice level, occasion, mood — via OpenRouter LLM with local `HeuristicIntentParser` fallback.

**Grocery Flow**
When `mealType == grocery`, `ResponseOrchestrator` routes to `getInstamartItems` and `extractIngredients`, returning a shopping list card with direct Instamart deep link. No restaurant cards shown.

**Food DNA**
After 3+ conversations, builds a personal taste profile from `ChatHistory` — spice tolerance, diet preference, average budget, ordering patterns, top cuisines. Fully local computation. Shareable as a card.

**Smart Location**
Detects city via device location. Currently supports Ahmedabad, Mumbai, and Bangalore with curated mock data. `UserIntent` already carries location context — passing live coordinates to Swiggy Food API requires no architectural change.

**Conversation History**
Every session persisted in `AppDatabase` with full `OrchestratedResponse`. Tap any history item to restore the complete conversation including restaurant cards — no re-querying.

**Response Resilience**
`ResponseOrchestrator` guarantees a result on every query. `isLlmOffline` flag drives UI state honestly — users see "Top Rated" not "AI-Powered" when the LLM is unavailable.

---

## Running Locally

```bash
git clone https://github.com/rudradave1/SwiggyMind
cd SwiggyMind
```

Add your free OpenRouter key to `local.properties` (never committed):
```
OPENROUTER_API_KEY=sk-or-xxxxxxxxxxxxxxxx
```

Get a free key at [openrouter.ai](https://openrouter.ai) — no credit card required.

```bash
./gradlew :androidApp:assembleDebug
```

> The app works fully without an OpenRouter key. `ResponseOrchestrator` falls back to `HeuristicIntentParser` automatically — all features remain functional.

---

## Built by

**Rudra Dave** — Senior Android Engineer · 6 years · Kotlin · KMP · Jetpack Compose

[![LinkedIn](https://img.shields.io/badge/LinkedIn-rudradave-0A66C2?style=flat-square&logo=linkedin)](https://linkedin.com/in/rudradave)
[![GitHub](https://img.shields.io/badge/GitHub-rudradave1-181717?style=flat-square&logo=github)](https://github.com/rudradave1)

Interested in joining Swiggy? So am I.

---

<p align="center">
  <sub>Built for Swiggy Builders Club · Not an official Swiggy product</sub>
</p>
