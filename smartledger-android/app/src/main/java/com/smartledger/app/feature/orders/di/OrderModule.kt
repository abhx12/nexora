package com.smartledger.app.feature.orders.di

import com.smartledger.app.feature.orders.data.api.OrderApiService
import com.smartledger.app.feature.orders.data.repository.OrderRepositoryImpl
import com.smartledger.app.feature.orders.domain.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrderModule {

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    companion object {
        @Provides
        @Singleton
        fun provideOrderApiService(retrofit: Retrofit): OrderApiService {
            return retrofit.create(OrderApiService::class.java)
        }
    }
}
