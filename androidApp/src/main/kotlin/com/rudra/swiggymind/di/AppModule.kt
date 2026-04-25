package com.rudra.swiggymind.di

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rudra.swiggymind.BuildConfig
import com.rudra.swiggymind.ai.LLMClient
import com.rudra.swiggymind.ai.OpenRouterClient
import com.rudra.swiggymind.data.local.AppDatabase
import com.rudra.swiggymind.data.local.ChatHistoryDao
import com.rudra.swiggymind.data.local.getDatabaseBuilder
import com.rudra.swiggymind.data.repository.AndroidSettingsRepository
import com.rudra.swiggymind.data.repository.MockRestaurantRepository
import com.rudra.swiggymind.data.repository.RestaurantRepository
import com.rudra.swiggymind.domain.repository.SettingsRepository
import com.rudra.swiggymind.domain.usecase.GetAIRecommendationsUseCase
import com.rudra.swiggymind.domain.usecase.ParseIntentUseCase
import com.rudra.swiggymind.domain.usecase.ResponseOrchestrator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return AndroidSettingsRepository(context, BuildConfig.OPENROUTER_API_KEY)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return getDatabaseBuilder(context)
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatHistoryDao(database: AppDatabase): ChatHistoryDao {
        return database.chatHistoryDao()
    }

    @Provides
    fun provideLLMClient(settingsRepository: SettingsRepository): LLMClient {
        val apiKey = settingsRepository.openRouterApiKey.value
        return OpenRouterClient(apiKey)
    }

    @Provides
    @Singleton
    fun provideRestaurantRepository(settingsRepository: SettingsRepository): RestaurantRepository {
        return MockRestaurantRepository(settingsRepository)
    }

    @Provides
    fun provideParseIntentUseCase(settingsRepository: SettingsRepository): ParseIntentUseCase {
        return ParseIntentUseCase(settingsRepository)
    }

    @Provides
    fun provideGetAIRecommendationsUseCase(
        settingsRepository: SettingsRepository,
        restaurantRepository: RestaurantRepository
    ): GetAIRecommendationsUseCase {
        return GetAIRecommendationsUseCase(settingsRepository, restaurantRepository)
    }

    @Provides
    fun provideResponseOrchestrator(
        settingsRepository: SettingsRepository,
        restaurantRepository: RestaurantRepository
    ): ResponseOrchestrator {
        return ResponseOrchestrator(settingsRepository, restaurantRepository)
    }
}
