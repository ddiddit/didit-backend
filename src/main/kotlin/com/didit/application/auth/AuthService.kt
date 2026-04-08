package com.didit.application.auth

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.dto.RefreshResponse
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.exception.ExpiredRefreshTokenException
import com.didit.application.auth.exception.InvalidRefreshTokenException
import com.didit.application.auth.provided.Auth
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.TokenProvider
import com.didit.application.auth.required.UserRepository
import com.didit.application.auth.required.WithdrawalRecordRepository
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.auth.Provider
import com.didit.domain.auth.RefreshToken
import com.didit.domain.auth.User
import com.didit.domain.auth.UserRegisterRequest
import com.didit.domain.auth.WithdrawalReason
import com.didit.domain.auth.WithdrawalRecord
import org.slf4j.LoggerFactory
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
    private val withdrawalRecordRepository: WithdrawalRecordRepository,
    private val auditLogger: AuditLogger,
    private val deviceTokenRepository: DeviceTokenRepository,
) : Auth {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
    }

    @Transactional
    override fun login(
        provider: Provider,
        oauthToken: String,
    ): TokenResponse {
        val client = oAuthClientFactory.getClient(provider)

        val userInfo = client.getUserInfo(oauthToken)

        val (user, isNewUser) = resolveUser(provider, userInfo.providerId, userInfo.email)

        logger.info("로그인 성공 - userId: ${user.id}, provider: $provider, isNewUser: $isNewUser")

        auditLogger.log(
            actorId = user.id,
            actorType = ActorType.USER,
            action = AuditAction.USER_LOGGED_IN,
            payload = mapOf("provider" to provider.name),
        )

        return issueTokens(user, isNewUser)
    }

    @Transactional
    override fun logout(userId: UUID) {
        refreshTokenRepository.deleteByUserId(userId)

        logger.info("로그아웃 - userId: $userId")
    }

    @Transactional
    override fun withdraw(
        userId: UUID,
        reason: WithdrawalReason,
        reasonDetail: String?,
    ) {
        logger.info("회원 탈퇴 - userId: $userId, reason: $reason")

        val user = userFinder.findByIdOrThrow(userId)

        user.withdraw()

        userRepository.save(user)

        deviceTokenRepository.deleteByUserId(user.id)

        refreshTokenRepository.deleteByUserId(userId)

        withdrawalRecordRepository.save(
            WithdrawalRecord.create(
                userId = userId,
                reason = reason,
                reasonDetail = reasonDetail,
            ),
        )

        auditLogger.log(
            actorId = userId,
            actorType = ActorType.USER,
            action = AuditAction.USER_WITHDREW,
            payload =
                mapOf(
                    "reason" to reason.name,
                    "reasonDetail" to (reasonDetail ?: ""),
                ),
        )
    }

    @Transactional
    override fun refresh(refreshToken: String): RefreshResponse {
        val storedToken =
            refreshTokenRepository.findByToken(refreshToken)
                ?: throw InvalidRefreshTokenException()

        if (storedToken.isExpired()) throw ExpiredRefreshTokenException()

        val user = userFinder.findByIdOrThrow(storedToken.userId)

        val newRefreshToken = tokenProvider.generateRefreshToken()
        storedToken.rotate(newRefreshToken, tokenProvider.getRefreshTokenExpiresAt())

        refreshTokenRepository.save(storedToken)

        logger.debug("토큰 재발급 - userId: ${user.id}")

        return RefreshResponse(
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

        return existingUser to false
    }

    private fun createNewUser(
        provider: Provider,
        providerId: String,
        email: String?,
    ): User {
        logger.info("신규 유저 생성 - provider: $provider, providerId: $providerId")

        return userRepository.save(
            User.register(UserRegisterRequest(provider = provider, providerId = providerId, email = email)),
        )
    }

    private fun rejoinUser(user: User): User {
        logger.info("재가입 유저 - userId: ${user.id}")

        user.rejoin()

        return userRepository.save(user)
    }

    private fun issueTokens(
        user: User,
        isNewUser: Boolean,
    ): TokenResponse {
        refreshTokenRepository.deleteByUserId(user.id)

        val newRefreshToken = tokenProvider.generateRefreshToken()

        refreshTokenRepository.save(
            RefreshToken.create(user.id, newRefreshToken, tokenProvider.getRefreshTokenExpiresAt()),
        )

        return TokenResponse(
            accessToken = tokenProvider.generateAccessToken(user.id),
            refreshToken = newRefreshToken,
            isNewUser = isNewUser,
            isOnboardingCompleted = user.isOnboardingCompleted,
        )
    }
}
