# SwiggyMind 🧠

> A Context-Aware Reasoning Engine for Hyper-Personalized Food Discovery.

<p align="center">
  <img src="https://github.com/user-attachments/assets/51aa3505-83f1-4c7b-a480-186e996bdfe3" width="280" alt="SwiggyMind Demo"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/KMP-Multiplatform-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/AI-Reasoning%20Engine-FC8019?style=flat-square"/>
  <img src="https://img.shields.io/badge/Built%20for-Swiggy%20Builders%20Club-FC8019?style=flat-square"/>
</p>

Built for the **Swiggy Builders Club**, SwiggyMind moves beyond simple keyword search. It implements an intelligence layer that understands user intent, reasons through options, and surfaces recommendations with visible logic.

---

## 🚀 What Makes It Different?

SwiggyMind is not just a chat wrapper. It's a **Decision Engine** built on two core technical pillars that solve the "choice paralysis" of modern food apps:

### 1. The Deterministic Ranking Layer (Mind Engine)
Most AI apps pass a raw query to an LLM and hope for the best. SwiggyMind uses a **Weighted Ranking Algorithm** *before* the AI even sees the results.
- **How it works**: It parses natural language into structured signals (Diet, Budget, Speed, Spice) and scores restaurant candidates against these constraints.
- **Visible Reasoning**: Every card shows the exact "Win Condition": `Matches Spicy · Under ₹200 · Rated 4.7★ · 18 min delivery`.

### 2. The Mind Cache (Stateful Session Memory)
SwiggyMind remembers context across turns. It doesn't treat every message as a new search; it understands **Refinement**.
- **The Turn Logic**: Ask for *"something spicy"* and then follow up with *"make it veg."* The engine merges these constraints using stateful session memory.
- **Tangible UI**: When stateful reasoning is active, the app displays a `Refined from last search` badge, proving the engine is listening and adjusting.

---

## 🎯 Real-World Scenarios

### The "Late Night Budget" Constraint
- **User**: "Hungry, something spicy but under ₹200, fast delivery please."
- **Result**: `Spicy Paneer Wrap + 18 min delivery → Matches Spicy · Under ₹200`.

### The "Health-Conscious Discovery"
- **User**: "Show me some high-protein veg options."
- **Follow-up**: "make it quick."
- **Result**: `Paneer Tikka Salad + 12 min delivery → Refined · High Protein`.

---

## 🛠 Engineering Highlights

- **Neural Intent Parser**: Uses LLM to extract high-fidelity JSON (Mood, Spice Level, Occasion) from natural language.
- **Robust Fallback Strategy**: 3-layer execution stack (Cloud LLM → Rule-Based → Heuristic) ensures the app works offline or when API limits are reached.
- **Kotlin Multiplatform (KMP)**: 100% of the Mind Engine logic is shared between platforms.
- **Structured Discovery**: Enforced JSON schemas ensure zero-hallucination results.

## 👨‍💻 Built by
**Rudra Dave** — Senior Android Engineer
*Dedicated to building the next generation of food discovery at Swiggy.*

---
<sub>Built for Swiggy Builders Club · Not an official Swiggy product</sub>
