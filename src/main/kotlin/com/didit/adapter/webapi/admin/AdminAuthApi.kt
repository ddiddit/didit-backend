package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.admin.dto.AdminLoginRequest
import com.didit.adapter.webapi.admin.dto.AdminLoginResponse
import com.didit.adapter.webapi.admin.dto.AdminTokenRefreshRequest
import com.didit.adapter.webapi.admin.dto.AdminTokenRefreshResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminAuth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/auth")
@RestController
class AdminAuthApi(
    private val adminAuth: AdminAuth,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: AdminLoginRequest,
    ): SuccessResponse<AdminLoginResponse> {
        val result = adminAuth.login(request.email, request.password)
        return SuccessResponse.of(
            AdminLoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
            ),
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: AdminTokenRefreshRequest,
    ): SuccessResponse<AdminTokenRefreshResponse> {
        val result = adminAuth.refresh(request.refreshToken)
        return SuccessResponse.of(
            AdminTokenRefreshResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
            ),
        )
    }

    @RequireAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    fun logout(
        @CurrentAdminId adminId: UUID,
    ) {
        adminAuth.logout(adminId)
    }
}
