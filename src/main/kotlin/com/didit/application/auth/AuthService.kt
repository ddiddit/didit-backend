package com.didit.application.auth

import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.exception.ExpiredRefreshTokenException
import com.didit.application.auth.exception.InvalidRefreshTokenException
import com.didit.application.auth.provided.Auth
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.TokenProvider
import com.didit.application.auth.required.UserRepository
import com.didit.domain.auth.Provider
import com.didit.domain.auth.RefreshToken
import com.didit.domain.auth.User
import com.didit.domain.auth.UserRegisterRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userFinder: UserFinder,
    private val oAuthClientFactory: OAuthClientFactory,
    private val tokenProvider: TokenProvider,
) : Auth {
    @Transactional
    override fun login(
        provider: Provider,
        oauthToken: String,
    ): TokenResponse {
        val client = oAuthClientFactory.getClient(provider)
        val userInfo = client.getUserInfo(oauthToken)
        val (user, isNewUser) = resolveUser(provider, userInfo.providerId, userInfo.email)
        return issueTokens(user.id, isNewUser)
    }

    @Transactional
    override fun logout(userId: UUID) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    @Transactional
    override fun withdraw(userId: UUID) {
        val user = userFinder.findByIdOrThrow(userId)
        user.withdraw()
        userRepository.save(user)
        refreshTokenRepository.deleteByUserId(userId)
    }

    @Transactional
    override fun refresh(refreshToken: String): TokenResponse {
        val storedToken =
            refreshTokenRepository.findByToken(refreshToken)
                ?: throw InvalidRefreshTokenException()

        if (storedToken.isExpired()) throw ExpiredRefreshTokenException()

        val user = userFinder.findByIdOrThrow(storedToken.userId)
        val newRefreshToken = tokenProvider.generateRefreshToken()
        storedToken.rotate(newRefreshToken, tokenProvider.getRefreshTokenExpiresAt())
        refreshTokenRepository.save(storedToken)

        return TokenResponse(
            accessToken = tokenProvider.generateAccessToken(user.id),
            refreshToken = newRefreshToken,
        )
    }

    private fun resolveUser(
        provider: Provider,
        providerId: String,
        email: String?,
    ): Pair<User, Boolean> {
        val existingUser =
            userRepository.findByProviderAndProviderId(provider, providerId)
                ?: return createNewUser(provider, providerId, email) to true

        if (existingUser.isDeleted) return rejoinUser(existingUser) to true

        return existingUser to false
    }

    private fun createNewUser(
        provider: Provider,
        providerId: String,
        email: String?,
    ): User =
        userRepository.save(
            User.register(UserRegisterRequest(provider = provider, providerId = providerId, email = email)),
        )

    private fun rejoinUser(user: User): User {
        user.rejoin()
        return userRepository.save(user)
    }

    private fun issueTokens(
        userId: UUID,
        isNewUser: Boolean,
    ): TokenResponse {
        refreshTokenRepository.deleteByUserId(userId)
        val newRefreshToken = tokenProvider.generateRefreshToken()
        refreshTokenRepository.save(
            RefreshToken.create(userId, newRefreshToken, tokenProvider.getRefreshTokenExpiresAt()),
        )
        return TokenResponse(
            accessToken = tokenProvider.generateAccessToken(userId),
            refreshToken = newRefreshToken,
            isNewUser = isNewUser,
        )
    }
}
