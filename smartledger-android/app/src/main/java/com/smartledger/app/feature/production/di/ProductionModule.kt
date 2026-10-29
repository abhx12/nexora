package com.smartledger.app.feature.production.di

import com.smartledger.app.feature.production.data.api.ProductionApiService
import com.smartledger.app.feature.production.data.repository.ProductionRepositoryImpl
import com.smartledger.app.feature.production.domain.ProductionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductionModule {

    @Binds
    @Singleton
    abstract fun bindProductionRepository(impl: ProductionRepositoryImpl): ProductionRepository

    companion object {
        @Provides
        @Singleton
        fun provideProductionApiService(retrofit: Retrofit): ProductionApiService {
            return retrofit.create(ProductionApiService::class.java)
        }
    }
}
