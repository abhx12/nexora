package com.smartledger.app.feature.orders.domain

import com.smartledger.app.feature.orders.data.dto.CreateOrderRequest
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.data.dto.UpdateOrderRequest
import com.smartledger.app.feature.orders.domain.model.OrderStatus

/**
 * Domain repository contract for Customer Warp Orders.
 */
interface OrderRepository {

    suspend fun getOrders(
        customerId: String? = null,
        status: OrderStatus? = null
    ): Result<List<OrderResponse>>

    suspend fun getOrderById(id: String): Result<OrderResponse>

    suspend fun createOrder(request: CreateOrderRequest): Result<OrderResponse>

    suspend fun updateOrder(id: String, request: UpdateOrderRequest): Result<OrderResponse>

    suspend fun lockOrder(id: String): Result<OrderResponse>

    suspend fun cancelOrder(id: String): Result<OrderResponse>
}
