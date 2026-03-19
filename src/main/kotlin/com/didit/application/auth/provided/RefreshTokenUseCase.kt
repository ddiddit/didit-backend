package com.didit.application.auth.provided

import com.didit.application.auth.dto.TokenInfo
import com.didit.application.auth.port.JwtPort
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtPort: JwtPort,
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
) {
    @Transactional
    fun refresh(refreshToken: String): TokenInfo {
        val userId = jwtPort.getUserId(refreshToken)

        val savedToken =
            refreshTokenRepository.findByUserId(userId)
                ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)

        if (savedToken.token != refreshToken) {
            throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

        if (savedToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN)
        }

        val user =
            userRepository.findById(savedToken.userId)
                ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        val newAccessToken = jwtPort.createAccessToken(user.id, user.role)
        val newRefreshToken = jwtPort.createRefreshToken(user.id)

        savedToken.token = newRefreshToken
        savedToken.expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration)

        refreshTokenRepository.save(savedToken)

        return TokenInfo(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }
}
