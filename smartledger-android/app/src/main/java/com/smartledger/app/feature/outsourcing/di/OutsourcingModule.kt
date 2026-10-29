package com.smartledger.app.feature.outsourcing.di

import com.smartledger.app.feature.outsourcing.data.api.OutsourcingApiService
import com.smartledger.app.feature.outsourcing.data.repository.OutsourcingRepositoryImpl
import com.smartledger.app.feature.outsourcing.domain.OutsourcingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OutsourcingModule {

    @Binds
    @Singleton
    abstract fun bindOutsourcingRepository(impl: OutsourcingRepositoryImpl): OutsourcingRepository

    companion object {
        @Provides
        @Singleton
        fun provideOutsourcingApiService(retrofit: Retrofit): OutsourcingApiService {
            return retrofit.create(OutsourcingApiService::class.java)
        }
    }
}
