package com.rudra.swiggymind.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val openRouterApiKey: StateFlow<String>
    val isMockDataEnabled: StateFlow<Boolean>
    val hasDismissedOfflineBanner: StateFlow<Boolean>
    val currentCity: StateFlow<String>

    suspend fun setOpenRouterApiKey(key: String)
    suspend fun setMockDataEnabled(enabled: Boolean)
    suspend fun setOfflineBannerDismissed(dismissed: Boolean)
    suspend fun setCurrentCity(city: String)
}

enum class AiMode {
    LOCAL, CLOUD
}
