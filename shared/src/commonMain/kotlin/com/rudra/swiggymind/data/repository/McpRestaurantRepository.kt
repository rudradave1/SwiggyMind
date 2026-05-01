package com.rudra.swiggymind.data.repository

import com.rudra.swiggymind.AppConstants
import com.rudra.swiggymind.ai.AiJsonParser
import com.rudra.swiggymind.ai.McpClient
import com.rudra.swiggymind.domain.model.InstamartItem
import com.rudra.swiggymind.domain.model.Restaurant
import com.rudra.swiggymind.domain.model.UserIntent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Live implementation of [RestaurantRepository] that calls Swiggy Builders Club MCP servers.
 *
 * Local development (stub):   baseUrl = AppConstants.MCP_BASE_URL_LOCAL  ("http://10.0.2.2:3000")
 * Staging / production:       baseUrl = AppConstants.MCP_BASE_URL_STAGING ("https://mcp.swiggy.com")
 *
 * This class is injected by AppModule when BuildConfig.USE_MCP_BACKEND = true.
 * The interface contract is identical to MockRestaurantRepository — ResponseOrchestrator
 * and all fallback layers are completely unaware of which implementation is active.
 */
class McpRestaurantRepository(
    accessToken: String,
    useStaging: Boolean = false
) : RestaurantRepository {

    private val baseUrl = if (useStaging) AppConstants.MCP_BASE_URL_STAGING
                          else AppConstants.MCP_BASE_URL_LOCAL

    private val client = McpClient(accessToken = accessToken, baseUrl = baseUrl)

    /** In-memory cache keyed by restaurant ID for getRestaurantById look-ups. */
    private val restaurantCache = mutableMapOf<String, Restaurant>()

    /** Cached delivery address ID — resolved once per session. */
    private var cachedAddressId: String? = null

    // ── RestaurantRepository ──────────────────────────────────────────────────

    override suspend fun getRestaurants(intent: UserIntent?): List<Restaurant> {
        val addressId = resolveAddressId()
        val query = intent?.buildSearchQuery() ?: ""

        val raw = client.callTool(
            serverPath = "/food",
            toolName = "search_restaurants",
            arguments = buildJsonObject {
                put("addressId", addressId)
                put("query", query)
            }
        ) ?: return emptyList()

        val response = AiJsonParser.decodeOrNull<SwiggyFoodSearchResponse>(raw) ?: return emptyList()
        return response.data.restaurants
            .filter { it.availabilityStatus == "OPEN" }
            .map { it.toDomain() }
            .also { it.forEach { r -> restaurantCache[r.id] = r } }
    }

    override suspend fun getRestaurantById(id: String): Restaurant? {
        // Served from cache populated by prior getRestaurants / getDineoutVenues calls.
        return restaurantCache[id]
    }

    override suspend fun getInstamartItems(intent: UserIntent?): List<InstamartItem> {
        val addressId = resolveAddressId()
        val query = intent?.buildSearchQuery() ?: "grocery"

        val raw = client.callTool(
            serverPath = "/im",
            toolName = "search_products",
            arguments = buildJsonObject {
                put("addressId", addressId)
                put("query", query)
            }
        ) ?: return emptyList()

        val response = AiJsonParser.decodeOrNull<SwiggyInstamartSearchResponse>(raw) ?: return emptyList()
        return response.data.products.map { it.toDomain() }
    }

    override suspend fun getDineoutVenues(intent: UserIntent?): List<Restaurant> {
        val query = intent?.buildSearchQuery() ?: ""

        val raw = client.callTool(
            serverPath = "/dineout",
            toolName = "search_restaurants_dineout",
            arguments = buildJsonObject {
                put("query", query)
            }
        ) ?: return emptyList()

        val response = AiJsonParser.decodeOrNull<SwiggyDineoutSearchResponse>(raw) ?: return emptyList()
        return response.data.restaurants
            .map { it.toDomain() }
            .also { it.forEach { r -> restaurantCache[r.id] = r } }
    }

    // ── Address resolution ────────────────────────────────────────────────────

    private suspend fun resolveAddressId(): String {
        cachedAddressId?.let { return it }

        val raw = client.callTool(serverPath = "/food", toolName = "get_addresses")
        val response = raw?.let { AiJsonParser.decodeOrNull<SwiggyAddressResponse>(it) }
        val id = response?.data?.addresses?.firstOrNull()?.id ?: AppConstants.MCP_FALLBACK_ADDRESS_ID
        cachedAddressId = id
        return id
    }

    // ── Wire models (match Swiggy MCP response schema) ───────────────────────

    @Serializable
    private data class SwiggyAddressResponse(val success: Boolean = false, val data: AddressData = AddressData())
    @Serializable
    private data class AddressData(val addresses: List<SwiggyAddress> = emptyList())
    @Serializable
    private data class SwiggyAddress(val id: String, val label: String = "", val displayAddress: String = "")

    @Serializable
    private data class SwiggyFoodSearchResponse(val success: Boolean = false, val data: FoodData = FoodData())
    @Serializable
    private data class FoodData(val restaurants: List<SwiggyRestaurant> = emptyList())
    @Serializable
    private data class SwiggyRestaurant(
        val id: String,
        val name: String,
        val cuisines: List<String> = emptyList(),
        val avgRating: Double = 0.0,
        val sla: Sla = Sla(),
        val costForTwo: Int = 0,
        val imageUrl: String? = null,
        val cloudinaryImageId: String? = null,
        val isVeg: Boolean = false,
        val availabilityStatus: String = "OPEN",
        val locality: String? = null,
        val tags: List<String> = emptyList()
    )
    @Serializable
    private data class Sla(val deliveryTime: Int = 30)

    @Serializable
    private data class SwiggyInstamartSearchResponse(val success: Boolean = false, val data: InstamartData = InstamartData())
    @Serializable
    private data class InstamartData(val products: List<SwiggyProduct> = emptyList())
    @Serializable
    private data class SwiggyProduct(
        val id: String,
        val name: String,
        val category: String = "Grocery",
        val price: Double = 0.0,
        val imageUrl: String? = null,
        val inStock: Boolean = true
    )

    @Serializable
    private data class SwiggyDineoutSearchResponse(val success: Boolean = false, val data: DineoutData = DineoutData())
    @Serializable
    private data class DineoutData(val restaurants: List<SwiggyDineoutRestaurant> = emptyList())
    @Serializable
    private data class SwiggyDineoutRestaurant(
        val id: String,
        val name: String,
        val cuisines: List<String> = emptyList(),
        val rating: Double = 0.0,
        val costForTwo: Int = 0,
        val imageUrl: String? = null,
        val area: String? = null,
        val tags: List<String> = emptyList()
    )

    // ── Domain mappers ────────────────────────────────────────────────────────

    private fun SwiggyRestaurant.toDomain() = Restaurant(
        id = id,
        name = name,
        cuisine = cuisines,
        rating = avgRating,
        deliveryTimeMinutes = sla.deliveryTime,
        costForTwo = costForTwo,
        imageUrl = imageUrl ?: buildCloudinaryUrl(cloudinaryImageId),
        isVeg = isVeg,
        isOpen = availabilityStatus == "OPEN",
        tags = tags,
        location = locality
    )

    private fun SwiggyProduct.toDomain() = InstamartItem(
        id = id,
        name = name,
        category = category,
        price = price.toInt(),
        rating = 4.0,
        deliveryTimeMinutes = AppConstants.INSTAMART_DELIVERY_MINS,
        imageUrl = imageUrl ?: AppConstants.FALLBACK_IMAGE_URL
    )

    private fun SwiggyDineoutRestaurant.toDomain() = Restaurant(
        id = id,
        name = name,
        cuisine = cuisines,
        rating = rating,
        deliveryTimeMinutes = 0,
        costForTwo = costForTwo,
        imageUrl = imageUrl ?: AppConstants.FALLBACK_DINEOUT_IMAGE_URL,
        isDineout = true,
        tags = tags,
        location = area
    )

    private fun buildCloudinaryUrl(imageId: String?): String =
        if (imageId != null)
            "https://media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto,w_660/$imageId"
        else
            AppConstants.FALLBACK_IMAGE_URL
}

// ── Intent → search query ─────────────────────────────────────────────────────

private fun UserIntent.buildSearchQuery(): String = buildString {
    specificCravings.firstOrNull()?.let { append(it) }
    dietaryPreference?.let { if (isNotEmpty()) append(" "); append(it) }
    budget?.let { if (isNotEmpty()) append(" "); append("under $it") }
}.trim()
