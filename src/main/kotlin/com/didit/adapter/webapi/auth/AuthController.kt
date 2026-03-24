package com.didit.adapter.webapi.auth

import com.didit.adapter.auth.security.CustomUserDetails
import com.didit.adapter.webapi.auth.dto.TokenResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.dto.RefreshTokenRequest
import com.didit.application.auth.dto.SocialLoginRequest
import com.didit.application.auth.provided.LogoutUseCase
import com.didit.application.auth.provided.RefreshTokenUseCase
import com.didit.application.auth.provided.SocialLoginUseCase
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/auth")
class AuthController(
    private val socialLoginUseCase: SocialLoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
) {
    @PostMapping("/login")
    fun socialLogin(
        @RequestBody request: SocialLoginRequest,
    ): SuccessResponse<TokenResponse> {
        val useResult =
            socialLoginUseCase.login(
                provider = request.provider,
                idToken = request.idToken,
            )

        val result =
            TokenResponse(
                accessToken = useResult.accessToken,
                refreshToken = useResult.refreshToken,
            )

        return SuccessResponse.of(
            data = result,
        )
    }

    @GetMapping("/kakao/callback")
    fun kakaoCallback(
        @RequestParam code: String,
    ): SuccessResponse<TokenResponse> {
        val useResult =
            socialLoginUseCase.loginWithKakao(
                code = code,
                redirectUri = "http://localhost:8080/api/v1/auth/kakao/callback",
            )

        return SuccessResponse.of(
            data = TokenResponse(useResult.accessToken, useResult.refreshToken),
        )
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody request: RefreshTokenRequest,
    ): SuccessResponse<TokenResponse> {
        val useResult = refreshTokenUseCase.refresh(request.refreshToken)

        val result =
            TokenResponse(
                accessToken = useResult.accessToken,
                refreshToken = useResult.refreshToken,
            )

        return SuccessResponse.of(
            data = result,
        )
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): SuccessResponse<Void?> {
        logoutUseCase.logout(user.getUserId())

        return SuccessResponse.of(null)
    }
}
