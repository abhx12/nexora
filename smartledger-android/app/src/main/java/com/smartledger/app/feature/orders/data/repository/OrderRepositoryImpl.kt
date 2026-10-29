package com.smartledger.app.feature.orders.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.orders.data.api.OrderApiService
import com.smartledger.app.feature.orders.data.dto.CreateOrderRequest
import com.smartledger.app.feature.orders.data.dto.OrderItemResponse
import com.smartledger.app.feature.orders.data.dto.OrderResponse
import com.smartledger.app.feature.orders.data.dto.UpdateOrderRequest
import com.smartledger.app.feature.orders.domain.OrderRepository
import com.smartledger.app.feature.orders.domain.model.OrderStatus
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderApiService: OrderApiService,
    private val partiesRepository: PartiesRepository,
    private val gson: Gson
) : OrderRepository {

    // Thread-safe in-memory cache / mock data for standalone development
    private val mockOrders = CopyOnWriteArrayList<OrderResponse>().apply {
        addAll(
            listOf(
                OrderResponse(
                    id = "ord-1",
                    orderNumber = "ORD-2026-1001",
                    customerId = "cust-1",
                    customerName = "Ramesh Silks & Sarees",
                    orderDate = "2026-10-10",
                    requiredDate = "2026-10-25",
                    status = "CREATED",
                    isLocked = false,
                    notes = "High-priority festival warp requirement",
                    items = listOf(
                        OrderItemResponse(
                            id = "item-101",
                            orderId = "ord-1",
                            productType = "SILK_WARP",
                            requiredWeightKg = 50.000,
                            ratePerKg = 4800.00,
                            warpSpecification = "60s × 40s, 8-ply, 4000 ends",
                            amount = 240000.00
                        ),
                        OrderItemResponse(
                            id = "item-102",
                            orderId = "ord-1",
                            productType = "KORA_WARP",
                            requiredWeightKg = 30.000,
                            ratePerKg = 4400.00,
                            warpSpecification = "28/30 denier unboiled kora",
                            amount = 132000.00
                        )
                    )
                ),
                OrderResponse(
                    id = "ord-2",
                    orderNumber = "ORD-2026-1002",
                    customerId = "cust-2",
                    customerName = "Mysore Silk Weavers Co-op",
                    orderDate = "2026-10-12",
                    requiredDate = "2026-10-22",
                    status = "IN_PROGRESS",
                    isLocked = true,
                    notes = "Currently under warping at Loom Line 2",
                    items = listOf(
                        OrderItemResponse(
                            id = "item-103",
                            orderId = "ord-2",
                            productType = "SILK_WARP",
                            requiredWeightKg = 80.000,
                            ratePerKg = 4900.00,
                            warpSpecification = "Pure Mulberry 2-ply warp",
                            amount = 392000.00
                        )
                    )
                ),
                OrderResponse(
                    id = "ord-3",
                    orderNumber = "ORD-2026-1003",
                    customerId = "cust-1",
                    customerName = "Ramesh Silks & Sarees",
                    orderDate = "2026-10-14",
                    requiredDate = "2026-10-30",
                    status = "CONFIRMED",
                    isLocked = false,
                    notes = "Advance payment received, awaiting yarn allocation",
                    items = listOf(
                        OrderItemResponse(
                            id = "item-104",
                            orderId = "ord-3",
                            productType = "VUDA_WARP",
                            requiredWeightKg = 25.000,
                            ratePerKg = 4600.00,
                            warpSpecification = "Specialty Vuda Weft / Border warp",
                            amount = 115000.00
                        )
                    )
                ),
                OrderResponse(
                    id = "ord-4",
                    orderNumber = "ORD-2026-1004",
                    customerId = "cust-3",
                    customerName = "Bangalore Saree Emporium",
                    orderDate = "2026-09-25",
                    requiredDate = "2026-10-05",
                    status = "COMPLETED",
                    isLocked = true,
                    notes = "Dispatched and fully fulfilled",
                    items = listOf(
                        OrderItemResponse(
                            id = "item-105",
                            orderId = "ord-4",
                            productType = "SILK_WARP",
                            requiredWeightKg = 60.000,
                            ratePerKg = 4750.00,
                            warpSpecification = "Standard zari-ready silk warp",
                            amount = 285000.00
                        )
                    )
                )
            )
        )
    }

    override suspend fun getOrders(
        customerId: String?,
        status: OrderStatus?
    ): Result<List<OrderResponse>> {
        return try {
            val response = orderApiService.getOrders(
                customerId = customerId,
                status = status?.name
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                var list = mockOrders.toList()
                if (!customerId.isNullOrBlank()) {
                    list = list.filter { it.customerId == customerId }
                }
                if (status != null) {
                    list = list.filter { it.status.equals(status.name, ignoreCase = true) }
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderById(id: String): Result<OrderResponse> {
        return try {
            val response = orderApiService.getOrderById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val found = mockOrders.firstOrNull { it.id == id || it.orderNumber.equals(id, ignoreCase = true) }
                if (found != null) {
                    Result.success(found)
                } else {
                    Result.failure(Exception("Order not found"))
                }
            } else {
                Result.failure(Exception("Unable to fetch order: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createOrder(request: CreateOrderRequest): Result<OrderResponse> {
        if (request.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Order must have at least one line item"))
        }
        for ((idx, item) in request.items.withIndex()) {
            if (item.requiredWeightKg <= 0) {
                return Result.failure(IllegalArgumentException("Item #${idx + 1}: Required weight must be greater than 0 kg"))
            }
            if (item.ratePerKg <= 0) {
                return Result.failure(IllegalArgumentException("Item #${idx + 1}: Rate per kg must be greater than ₹0"))
            }
        }

        return try {
            val response = orderApiService.createOrder(request)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                mockOrders.add(0, created)
                Result.success(created)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val customerName = partiesRepository.getPartyById(request.customerId)
                    .getOrNull()?.name ?: "Customer"
                val orderNumber = "ORD-2026-%04d".format(mockOrders.size + 1001)
                val orderId = UUID.randomUUID().toString()

                val createdItems = request.items.mapIndexed { idx, itm ->
                    OrderItemResponse(
                        id = "item-${System.currentTimeMillis()}-$idx",
                        orderId = orderId,
                        productType = itm.productType,
                        requiredWeightKg = itm.requiredWeightKg,
                        ratePerKg = itm.ratePerKg,
                        warpSpecification = itm.warpSpecification,
                        amount = itm.requiredWeightKg * itm.ratePerKg
                    )
                }

                val created = OrderResponse(
                    id = orderId,
                    orderNumber = orderNumber,
                    customerId = request.customerId,
                    customerName = customerName,
                    orderDate = request.orderDate,
                    requiredDate = request.requiredDate,
                    status = "CREATED",
                    isLocked = false,
                    items = createdItems,
                    totalWeightKg = createdItems.sumOf { it.requiredWeightKg },
                    totalAmount = createdItems.sumOf { it.amount },
                    notes = request.notes
                )
                mockOrders.add(0, created)
                Result.success(created)
            } else {
                Result.failure(Exception("Unable to create order: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOrder(id: String, request: UpdateOrderRequest): Result<OrderResponse> {
        return try {
            val response = orderApiService.updateOrder(id, request)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                val idx = mockOrders.indexOfFirst { it.id == id }
                if (idx != -1) mockOrders[idx] = updated
                Result.success(updated)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val idx = mockOrders.indexOfFirst { it.id == id || it.orderNumber.equals(id, ignoreCase = true) }
                if (idx == -1) return Result.failure(Exception("Order not found"))

                val current = mockOrders[idx]
                // Locked order handling:
                if (current.isOrderLocked) {
                    return Result.failure(Exception("This order is in production and cannot be changed."))
                }

                val updatedItems = request.items?.mapIndexed { itemIdx, itm ->
                    OrderItemResponse(
                        id = "item-$itemIdx",
                        orderId = current.id,
                        productType = itm.productType,
                        requiredWeightKg = itm.requiredWeightKg,
                        ratePerKg = itm.ratePerKg,
                        warpSpecification = itm.warpSpecification,
                        amount = itm.requiredWeightKg * itm.ratePerKg
                    )
                } ?: current.items

                val updated = current.copy(
                    requiredDate = request.requiredDate ?: current.requiredDate,
                    notes = request.notes ?: current.notes,
                    items = updatedItems,
                    totalWeightKg = updatedItems.sumOf { it.requiredWeightKg },
                    totalAmount = updatedItems.sumOf { it.amount }
                )
                mockOrders[idx] = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Unable to update order: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lockOrder(id: String): Result<OrderResponse> {
        return try {
            val response = orderApiService.lockOrder(id)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                val idx = mockOrders.indexOfFirst { it.id == id }
                if (idx != -1) mockOrders[idx] = updated
                Result.success(updated)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val idx = mockOrders.indexOfFirst { it.id == id || it.orderNumber.equals(id, ignoreCase = true) }
                if (idx == -1) return Result.failure(Exception("Order not found"))

                val current = mockOrders[idx]
                val updated = current.copy(isLocked = true, status = "IN_PROGRESS")
                mockOrders[idx] = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Unable to lock order: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelOrder(id: String): Result<OrderResponse> {
        return try {
            val response = orderApiService.cancelOrder(id)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                val idx = mockOrders.indexOfFirst { it.id == id }
                if (idx != -1) mockOrders[idx] = updated
                Result.success(updated)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val idx = mockOrders.indexOfFirst { it.id == id || it.orderNumber.equals(id, ignoreCase = true) }
                if (idx == -1) return Result.failure(Exception("Order not found"))

                val current = mockOrders[idx]
                if (current.status.equals("IN_PROGRESS", ignoreCase = true) || current.status.equals("COMPLETED", ignoreCase = true)) {
                    return Result.failure(Exception("Cannot cancel an order that is in production or completed"))
                }
                val updated = current.copy(status = "CANCELLED")
                mockOrders[idx] = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Unable to cancel order: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (!errorBody.isNullOrBlank()) {
            try {
                val apiError = gson.fromJson(errorBody, ApiError::class.java)
                if (apiError?.code == "ORDER_LOCKED") {
                    return "This order is in production and cannot be changed."
                }
                if (!apiError?.message.isNullOrBlank()) {
                    return apiError.message
                }
            } catch (_: Exception) {
            }
        }
        return when (statusCode) {
            400 -> "Invalid customer order details"
            404 -> "Order not found"
            409 -> "This order is in production and cannot be changed."
            else -> "Order request failed (code: $statusCode)"
        }
    }
}
