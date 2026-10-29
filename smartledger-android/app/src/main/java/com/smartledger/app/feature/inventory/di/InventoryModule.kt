package com.smartledger.app.feature.inventory.di

import com.smartledger.app.feature.inventory.data.api.InventoryApiService
import com.smartledger.app.feature.inventory.data.repository.InventoryRepositoryImpl
import com.smartledger.app.feature.inventory.domain.InventoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryModule {

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideInventoryApiService(retrofit: Retrofit): InventoryApiService {
            return retrofit.create(InventoryApiService::class.java)
        }
    }
}
