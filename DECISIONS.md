# Engineering Decisions: SwiggyMind Mind Engine

This document outlines the high-level architectural trade-offs and design decisions made for the SwiggyMind intelligence layer.

### 1. Hybrid Architecture: LLM + Deterministic Ranking
- **Decision**: Decoupled "Intent Parsing" (probabilistic) from "Selection Ranking" (deterministic).
- **Reasoning**: LLMs are excellent at understanding nuance in natural language but unreliable for precise sorting or filtering against large datasets. By extracting intent into a structured JSON schema first, we can apply a consistent, weighted scoring algorithm that ensures reliability and zero hallucinations.

### 2. Progressive Resilience (The Fallback Chain)
- **Decision**: Implemented a 3-layer execution stack: `Cloud LLM` → `Rule-Based Mind Engine` → `Heuristic Fallback`.
- **Reasoning**: Production ordering systems cannot have a single point of failure. This architecture ensures that if an AI provider is rate-limited or the network is unstable, the system gracefully degrades to high-quality local reasoning rather than showing an error state.

### 3. Decision Quality over Simple Generation
- **Decision**: Used Chain-of-Thought (CoT) prompting to generate a "Cognitive Reasoning" block.
- **Reasoning**: To move from a "demo" to a "product," transparency is key. Users need to know *why* a specific restaurant was picked. This creates "Decision Quality" and builds trust in the AI's logic.

### 4. Stateful "Mind Memory"
- **Decision**: Injected pruned session context into every intent turn.
- **Reasoning**: Users don't repeat themselves. If a user asks for "veg only" in turn 1, that constraint must persist in turn 2. We manage this via a sliding-window context buffer that maintains state without bloating LLM token costs.

### 5. Multiplatform Architecture
- **Decision**: Shared 100% of the "Mind Engine" logic in Kotlin Multiplatform (KMP).
- **Reasoning**: Business logic for food discovery (ranking, intent schemas, fallback rules) is platform-agnostic. Keeping it in `commonMain` ensures a single source of truth for both Android and iOS, reducing bug surface area and development overhead.
