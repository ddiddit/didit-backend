package com.didit.application.auth.provided

import com.didit.application.auth.dto.TokenInfo
import com.didit.application.auth.exception.ExpiredRefreshTokenException
import com.didit.application.auth.exception.InvalidRefreshTokenException
import com.didit.application.auth.exception.UserNotFoundException
import com.didit.application.auth.port.JwtPort
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
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
                ?: throw InvalidRefreshTokenException()

        if (savedToken.token != refreshToken) {
            throw InvalidRefreshTokenException()
        }

        if (savedToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw ExpiredRefreshTokenException()
        }

        val user =
            userRepository.findById(savedToken.userId)
                ?: throw UserNotFoundException(savedToken.userId)

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
