package com.smartledger.app.feature.orders.data.api

import com.smartledger.app.feature.orders.data.dto.CreateOrderRequest
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.data.dto.UpdateOrderRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API contract for Customer Warp Orders.
 */
interface OrderApiService {

    @GET("api/v1/orders")
    suspend fun getOrders(
        @Query("customerId") customerId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<OrderResponse>>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderById(
        @Path("id") id: String
    ): Response<OrderResponse>

    @POST("api/v1/orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @PATCH("api/v1/orders/{id}")
    suspend fun updateOrder(
        @Path("id") id: String,
        @Body request: UpdateOrderRequest
    ): Response<OrderResponse>

    @POST("api/v1/orders/{id}/lock")
    suspend fun lockOrder(
        @Path("id") id: String
    ): Response<OrderResponse>

    @POST("api/v1/orders/{id}/cancel")
    suspend fun cancelOrder(
        @Path("id") id: String
    ): Response<OrderResponse>
}
