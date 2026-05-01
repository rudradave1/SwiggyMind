# SwiggyMind MCP Local Stub 🛠️

This is a Node.js implementation of the **Swiggy Builders Club MCP (Model Context Protocol)** server. It allows you to run SwiggyMind in "Live" mode without needing production API credentials.

## What it does
- Mirrors the `mcp.swiggy.com` JSON-RPC surface.
- Implements 10+ core Swiggy tools (`search_restaurants`, `get_addresses`, `update_cart`, etc.).
- Returns realistic mock data that follows the official Swiggy MCP schemas.
- Supports the Android Emulator (reaches localhost at `10.0.2.2`).

## Quick Start

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Start the server**:
   ```bash
   npm start
   ```
   The server will run on `http://localhost:3000`.

3. **Configure the Android app**:
   In `androidApp/build.gradle.kts`, set:
   ```kotlin
   buildConfigField("Boolean", "USE_MCP_BACKEND", "true")
   ```

## Supported Endpoints
- **Food**: `POST /food`
- **Instamart**: `POST /im`
- **Dineout**: `POST /dineout`

## Why use this?
Swiggy's official MCP servers are strictly rate-limited and restricted to Builders Club members. This stub allows developers to:
- Test multi-turn cart logic.
- Verify address resolution.
- Polish UI badges and loading states.
- Demo the app in a "Live" environment entirely offline.
