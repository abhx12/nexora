package com.smartledger.app.feature.dashboard.di

import com.smartledger.app.feature.dashboard.data.api.DashboardApiService
import com.smartledger.app.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.smartledger.app.feature.dashboard.domain.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    companion object {
        @Provides
        @Singleton
        fun provideDashboardApiService(retrofit: Retrofit): DashboardApiService {
            return retrofit.create(DashboardApiService::class.java)
        }
    }
}
