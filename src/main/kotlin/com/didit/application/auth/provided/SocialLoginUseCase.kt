package com.didit.application.auth.provided

import com.didit.application.auth.dto.TokenInfo
import com.didit.application.auth.port.JwtPort
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.auth.required.social.KakaoAuthPort
import com.didit.application.auth.required.social.SocialAuthPort
import com.didit.domain.auth.entity.RefreshToken
import com.didit.domain.auth.entity.User
import com.didit.domain.auth.enums.SocialProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SocialLoginUseCase(
    private val socialAuthPort: SocialAuthPort,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtPort: JwtPort,
    private val kakaoAuthPort: KakaoAuthPort,
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
) {
    @Transactional
    fun login(
        provider: SocialProvider,
        idToken: String,
    ): TokenInfo {
        val socialUser = socialAuthPort.verifyIdToken(provider, idToken)

        var user =
            userRepository.findBySocialIdAndProvider(
                socialUser.socialId,
                provider,
            )

        if (user == null) {
            user =
                User.create(
                    provider = provider,
                    socialId = socialUser.socialId,
                    email = socialUser.email,
                )
            userRepository.save(user)
        }

        return issueToken(user)
    }

    @Transactional
    fun loginWithKakao(
        code: String,
        redirectUri: String,
    ): TokenInfo {
        val idToken = kakaoAuthPort.getIdToken(code, redirectUri)

        return login(
            provider = SocialProvider.KAKAO,
            idToken = idToken,
        )
    }

    private fun issueToken(user: User): TokenInfo {
        val userId = user.id
        val role = user.role

        val accessToken = jwtPort.createAccessToken(userId, role)
        val refreshToken = jwtPort.createRefreshToken(userId)

        val expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration)

        val existingToken = refreshTokenRepository.findByUserId(userId)

        val savedToken =
            existingToken?.apply {
                this.token = refreshToken
                this.expiresAt = expiresAt
            }
                ?: RefreshToken(
                    userId = userId,
                    token = refreshToken,
                    expiresAt = expiresAt,
                )

        refreshTokenRepository.save(savedToken)

        return TokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
