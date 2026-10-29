package com.smartledger.app.feature.parties.di

import com.smartledger.app.feature.parties.data.api.PartiesApiService
import com.smartledger.app.feature.parties.data.repository.PartiesRepositoryImpl
import com.smartledger.app.feature.parties.domain.PartiesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PartiesModule {

    @Binds
    @Singleton
    abstract fun bindPartiesRepository(impl: PartiesRepositoryImpl): PartiesRepository

    companion object {
        @Provides
        @Singleton
        fun providePartiesApiService(retrofit: Retrofit): PartiesApiService {
            return retrofit.create(PartiesApiService::class.java)
        }
    }
}
