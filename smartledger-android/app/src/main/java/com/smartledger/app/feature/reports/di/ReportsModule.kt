package com.smartledger.app.feature.reports.di

import com.smartledger.app.feature.reports.data.api.ReportsApiService
import com.smartledger.app.feature.reports.data.repository.ReportsRepositoryImpl
import com.smartledger.app.feature.reports.domain.ReportsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportsModule {

    @Binds
    @Singleton
    abstract fun bindReportsRepository(impl: ReportsRepositoryImpl): ReportsRepository

    companion object {
        @Provides
        @Singleton
        fun provideReportsApiService(retrofit: Retrofit): ReportsApiService {
            return retrofit.create(ReportsApiService::class.java)
        }
    }
}
