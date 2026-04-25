package com.rudra.swiggymind.data.repository

import com.rudra.swiggymind.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSUserDefaults

class IosSettingsRepository(private val openRouterKey: String) : SettingsRepository {
    private val defaults = NSUserDefaults.standardUserDefaults
    
    private val _openRouterApiKey = MutableStateFlow(openRouterKey)
    override val openRouterApiKey: StateFlow<String> = _openRouterApiKey

    private val _isMockDataEnabled = MutableStateFlow(true)
    override val isMockDataEnabled: StateFlow<Boolean> = _isMockDataEnabled

    private val _hasDismissedOfflineBanner = MutableStateFlow(defaults.boolForKey("offline_banner_dismissed"))
    override val hasDismissedOfflineBanner: StateFlow<Boolean> = _hasDismissedOfflineBanner

    private val _currentCity = MutableStateFlow(defaults.stringForKey("current_city") ?: "Ahmedabad")
    override val currentCity: StateFlow<String> = _currentCity

    override suspend fun setOpenRouterApiKey(key: String) {
        _openRouterApiKey.value = key
    }

    override suspend fun setMockDataEnabled(enabled: Boolean) {
        _isMockDataEnabled.value = enabled
    }

    override suspend fun setOfflineBannerDismissed(dismissed: Boolean) {
        defaults.setBool(dismissed, "offline_banner_dismissed")
        _hasDismissedOfflineBanner.value = dismissed
    }

    override suspend fun setCurrentCity(city: String) {
        defaults.setObject(city, "current_city")
        _currentCity.value = city
    }
}
