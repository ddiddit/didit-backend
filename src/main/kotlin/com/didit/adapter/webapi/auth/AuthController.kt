package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.auth.dto.TokenResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.dto.RefreshTokenRequest
import com.didit.application.auth.dto.SocialLoginRequest
import com.didit.application.auth.provided.RefreshTokenUseCase
import com.didit.application.auth.provided.SocialLoginUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val socialLoginUseCase: SocialLoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
) {
    @PostMapping("/social")
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
            message = "소셜 로그인에 성공했습니다.",
        )
    }

    @GetMapping("/social/kakao")
    fun kakaoCallback(
        @RequestParam code: String,
    ): SuccessResponse<TokenResponse> {
        val useResult =
            socialLoginUseCase.loginWithKakao(
                code = code,
                redirectUri = "http://localhost:8080/auth/social/kakao",
            )

        return SuccessResponse.of(
            data = TokenResponse(useResult.accessToken, useResult.refreshToken),
            message = "카카오 로그인 성공",
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
            message = "토큰 재발급에 성공했습니다.",
        )
    }
}
