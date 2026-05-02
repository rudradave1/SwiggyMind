# SwiggyMind 🧠

> A Context-Aware Reasoning Engine for Hyper-Personalized Food Discovery.

<p align="center">
  <img src="https://github.com/user-attachments/assets/51aa3505-83f1-4c7b-a480-186e996bdfe3" width="280" alt="SwiggyMind Demo"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/KMP-Multiplatform-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/AI-Decision%20Intelligence-FC8019?style=flat-square"/>
  <img src="https://img.shields.io/badge/Built%20for-Swiggy%20Builders%20Club-FC8019?style=flat-square"/>
</p>

Built for the **Swiggy Builders Club**, SwiggyMind moves beyond simple search. It implements an intelligence layer that understands user intent, reasons through options, and surfaces recommendations with undeniable decision-quality logic.

---

## 🚀 Key Engineering Highlights

### 1. Neural Intent Parser
Unlike traditional keyword search, SwiggyMind uses an **LLM-powered Neural Intent Parser**. It extracts high-fidelity structured data (JSON) from natural language, identifying:
- **Flavor Profiles**: Spicy, mild, healthy, indulgent.
- **Contextual Logistics**: Budget constraints, delivery speed, group size.
- **Mood Analysis**: Comfort food vs. adventurous dining.

### 2. Cognitive Mind Engine (Ranking System)
Implemented a **Weighted Ranking Algorithm** that scores restaurant candidates before they reach the AI.
- **DNA Fit (40%)**: Historical user preference alignment.
- **Intent Match (30%)**: Real-time craving satisfaction.
- **Operational Metrics (30%)**: Ratings, delivery time, and cost optimization.

### 3. Stateful "Mind Memory" & CoT
- **Contextual Persistence**: The engine remembers preferences across turns (e.g., "Veg only" persists into subsequent "Spicy" requests).
- **Decision Transparency**: Every recommendation explains its "Win Condition" (e.g., `High Protein + Vegetarian + Fast Delivery → Ranked #1`).
- **Reasoning Chain**: Displays the AI's internal logic for selection, reducing hallucinations via structured prompting.

### 4. Robust Hybrid Architecture
- **Multi-Layer Response Strategy**: Cloud LLM (OpenRouter) → Rule-based Ranking → Heuristic Fallback.
- **Semantic Consistency**: Enforced JSON schemas ensure zero-hallucination results and reliable UI rendering.

---

## 🎯 Real-World Scenarios (Decision Intelligence)

SwiggyMind handles complex, multi-constraint scenarios that standard search can't resolve:

### 1. The "Late Night Budget" Constraint
- **User**: "Hungry, something spicy but under ₹200, fast delivery please."
- **Mind Engine**: Filters for <25 min delivery + <₹400 cost for two + spicy tags.
- **Decision**: `Spicy Paneer Wrap + 18 min delivery → Ranked Top Pick` (Budget Optimized).

### 2. The "Health-Conscious Discovery"
- **User**: "Show me some high-protein veg options, not too heavy."
- **Mind Engine**: Analyzes menu metadata for low-calorie/high-protein markers (Tofu, Paneer, Salads).
- **Decision**: `Paneer Tikka Salad + 4.8 Rating → Ranked Top Pick` (Diet Style Alignment).

### 3. The "Group Ordering" Logic
- **User**: "Dinner for 4, need a mix of North and South Indian, pure veg."
- **Mind Engine**: Identifies multi-cuisine restaurants with high reliability for large orders.
- **Decision**: `Honest Restaurant + Multi-Cuisine + Family Portions → Ranked Top Pick` (Capacity Logic).

---

## 🛠 Tech Stack
- **Multiplatform**: Kotlin Multiplatform (KMP), Compose Multiplatform.
- **Persistence**: Room KMP (Chat History, Food DNA).
- **Networking**: Ktor Client with content negotiation.
- **AI Integration**: OpenRouter API with custom **Chain-of-Thought (CoT)** prompting.
- **DI**: Hilt (Android) & Manual Injection (Shared).

---

## 👨‍💻 Built by
**Rudra Dave** — Senior Android Engineer
*Dedicated to building the next generation of food discovery at Swiggy.*

---
<sub>Built for Swiggy Builders Club · Not an official Swiggy product</sub>
