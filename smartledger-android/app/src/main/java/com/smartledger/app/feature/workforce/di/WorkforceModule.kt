package com.smartledger.app.feature.workforce.di

import com.smartledger.app.feature.workforce.data.api.WorkforceApiService
import com.smartledger.app.feature.workforce.data.repository.WorkforceRepositoryImpl
import com.smartledger.app.feature.workforce.domain.WorkforceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkforceModule {

    @Binds
    @Singleton
    abstract fun bindWorkforceRepository(impl: WorkforceRepositoryImpl): WorkforceRepository

    companion object {
        @Provides
        @Singleton
        fun provideWorkforceApiService(retrofit: Retrofit): WorkforceApiService {
            return retrofit.create(WorkforceApiService::class.java)
        }
    }
}
