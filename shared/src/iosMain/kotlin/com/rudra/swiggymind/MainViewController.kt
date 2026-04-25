package com.rudra.swiggymind

import androidx.compose.ui.window.ComposeUIViewController
import com.rudra.swiggymind.di.SharedComponent
import com.rudra.swiggymind.di.sharedComponent
import com.rudra.swiggymind.data.local.getDatabaseBuilder
import com.rudra.swiggymind.data.repository.MockRestaurantRepository
import com.rudra.swiggymind.data.repository.IosSettingsRepository
import com.rudra.swiggymind.domain.usecase.ResponseOrchestrator
import com.rudra.swiggymind.ai.OpenRouterClient
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun MainViewController() = ComposeUIViewController {
    if (sharedComponent == null) {
        // Initialize dependencies for iOS
        val settingsRepository = IosSettingsRepository("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")
        val database = getDatabaseBuilder()
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .build()
        val chatHistoryDao = database.chatHistoryDao()
        val restaurantRepository = MockRestaurantRepository(settingsRepository)
        val responseOrchestrator = ResponseOrchestrator(settingsRepository, restaurantRepository)
        val llmClient = OpenRouterClient(settingsRepository.openRouterApiKey.value)
        
        sharedComponent = SharedComponent(
            restaurantRepository,
            chatHistoryDao,
            settingsRepository,
            responseOrchestrator,
            llmClient
        )
    }
    App() 
}
