package com.smartledger.app.feature.procurement.di

import com.smartledger.app.feature.procurement.data.api.PurchaseApiService
import com.smartledger.app.feature.procurement.data.repository.PurchaseRepositoryImpl
import com.smartledger.app.feature.procurement.domain.PurchaseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProcurementModule {

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(impl: PurchaseRepositoryImpl): PurchaseRepository

    companion object {
        @Provides
        @Singleton
        fun providePurchaseApiService(retrofit: Retrofit): PurchaseApiService {
            return retrofit.create(PurchaseApiService::class.java)
        }
    }
}
