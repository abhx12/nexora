package com.smartledger.app.feature.finance.di

import com.smartledger.app.feature.finance.data.api.FinanceApiService
import com.smartledger.app.feature.finance.data.repository.FinanceRepositoryImpl
import com.smartledger.app.feature.finance.domain.FinanceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FinanceModule {

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository

    companion object {
        @Provides
        @Singleton
        fun provideFinanceApiService(retrofit: Retrofit): FinanceApiService {
            return retrofit.create(FinanceApiService::class.java)
        }
    }
}
