package com.smartledger.app.feature.auth.di

import com.smartledger.app.core.auth.TokenManager
import com.smartledger.app.core.auth.TokenStorage
import com.smartledger.app.feature.auth.data.api.AuthApiService
import com.smartledger.app.feature.auth.data.repository.AuthRepositoryImpl
import com.smartledger.app.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTokenManager(impl: TokenStorage): TokenManager

    companion object {
        @Provides
        @Singleton
        fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
            return retrofit.create(AuthApiService::class.java)
        }
    }
}
