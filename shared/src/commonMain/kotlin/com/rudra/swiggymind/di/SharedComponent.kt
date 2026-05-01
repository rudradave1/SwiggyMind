package com.rudra.swiggymind.di

import com.rudra.swiggymind.data.local.ChatHistoryDao
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.domain.repository.SettingsRepository
import com.rudra.swiggymind.domain.usecase.ResponseOrchestrator
import com.rudra.swiggymind.ai.LLMClient

class SharedComponent(
    val restaurantRepository: RestaurantRepository,
    val chatHistoryDao: ChatHistoryDao,
    val settingsRepository: SettingsRepository,
    val responseOrchestrator: ResponseOrchestrator,
    val llmClient: LLMClient,
    val shouldSeedDefaults: Boolean = true,
    val isMcpEnabled: Boolean = false
)

var sharedComponent: SharedComponent? = null
