package com.rudra.swiggymind

import android.app.Application
import com.rudra.swiggymind.ai.LLMClient
import com.rudra.swiggymind.data.local.ChatHistoryDao
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.di.SharedComponent
import com.rudra.swiggymind.di.sharedComponent
import com.rudra.swiggymind.domain.repository.SettingsRepository
import com.rudra.swiggymind.domain.usecase.ResponseOrchestrator
import com.rudra.swiggymind.setPlatformContext
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SwiggyMindApp : Application() {
    
    @Inject lateinit var restaurantRepository: RestaurantRepository
    @Inject lateinit var chatHistoryDao: ChatHistoryDao
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var responseOrchestrator: ResponseOrchestrator
    @Inject lateinit var llmClient: LLMClient

    override fun onCreate() {
        super.onCreate()
        val isMcpEnabled = BuildConfig.USE_MCP_BACKEND
        val isAiAvailable = BuildConfig.OPENROUTER_API_KEY.isNotEmpty() && 
                           BuildConfig.OPENROUTER_API_KEY != "YOUR_KEY_HERE"
        
        sharedComponent = SharedComponent(
            restaurantRepository,
            chatHistoryDao,
            settingsRepository,
            responseOrchestrator,
            llmClient,
            shouldSeedDefaults = !isMcpEnabled && !isAiAvailable,
            isMcpEnabled = isMcpEnabled
        )
        setPlatformContext(this)
    }
}
