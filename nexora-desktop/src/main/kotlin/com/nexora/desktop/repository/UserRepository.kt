package com.nexora.desktop.repository

import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.api.dto.CreateUserRequest
import com.nexora.desktop.api.dto.ToggleUserStatusRequest
import com.nexora.desktop.api.dto.UserDto

class UserRepository(private val api: ApiClient) {

    suspend fun getUsers(): ApiResult<List<UserDto>> = api.get("/users")

    suspend fun createUser(req: CreateUserRequest): ApiResult<UserDto> = api.post("/users", req)

    suspend fun setUserStatus(userId: String, active: Boolean): ApiResult<UserDto> =
        api.patch("/users/$userId/status", ToggleUserStatusRequest(active))
}
