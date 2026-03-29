package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.auth.dto.LoginRequest
import com.didit.adapter.webapi.auth.dto.LoginResponse
import com.didit.adapter.webapi.auth.dto.TokenRefreshRequest
import com.didit.adapter.webapi.auth.dto.TokenRefreshResponse
import com.didit.adapter.webapi.auth.dto.WithdrawRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.provided.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/auth")
@RestController
class AuthApi(
    private val auth: Auth,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): SuccessResponse<LoginResponse> {
        val result = auth.login(request.provider, request.oauthToken)
        return SuccessResponse.of(
            LoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                isNewUser = result.isNewUser,
                isOnboardingCompleted = result.isOnboardingCompleted,
            ),
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: TokenRefreshRequest,
    ): SuccessResponse<TokenRefreshResponse> {
        val result = auth.refresh(request.refreshToken)
        return SuccessResponse.of(
            TokenRefreshResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
            ),
        )
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    @RequireAuth
    fun logout(
        @CurrentUserId userId: UUID,
    ) {
        auth.logout(userId)
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/withdraw")
    @RequireAuth
    fun withdraw(
        @CurrentUserId userId: UUID,
        @RequestBody request: WithdrawRequest,
    ) {
        auth.withdraw(
            userId = userId,
            reason = request.reason,
            reasonDetail = request.reasonDetail,
        )
    }
}
