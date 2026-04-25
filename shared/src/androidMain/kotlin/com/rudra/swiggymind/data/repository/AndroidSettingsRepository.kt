package com.rudra.swiggymind.data.repository

import android.content.Context
import com.rudra.swiggymind.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidSettingsRepository(
    private val context: Context,
    buildConfigApiKey: String = ""
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("swiggymind_settings", Context.MODE_PRIVATE)

    private val _hasDismissedOfflineBanner = MutableStateFlow(prefs.getBoolean("offline_banner_dismissed", false))
    override val hasDismissedOfflineBanner: StateFlow<Boolean> = _hasDismissedOfflineBanner

    private val _currentCity = MutableStateFlow(prefs.getString("current_city", "Ahmedabad") ?: "Ahmedabad")
    override val currentCity: StateFlow<String> = _currentCity

    // Priority: saved user key → BuildConfig key baked into APK
    private val _openRouterApiKey = MutableStateFlow(
        prefs.getString("open_router_key", "")
            ?.takeIf { it.isNotBlank() }
            ?: buildConfigApiKey
    )
    override val openRouterApiKey: StateFlow<String> = _openRouterApiKey

    override val isMockDataEnabled: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun setOpenRouterApiKey(key: String) {
        prefs.edit().putString("open_router_key", key.trim()).apply()
        _openRouterApiKey.value = key.trim()
    }
    override suspend fun setMockDataEnabled(enabled: Boolean) {}
    override suspend fun setOfflineBannerDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean("offline_banner_dismissed", dismissed).apply()
        _hasDismissedOfflineBanner.value = dismissed
    }
    override suspend fun setCurrentCity(city: String) {
        prefs.edit().putString("current_city", city).apply()
        _currentCity.value = city
    }
}
