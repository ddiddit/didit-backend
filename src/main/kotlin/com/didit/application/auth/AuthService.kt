package com.didit.application.auth

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.dto.RefreshResponse
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.exception.AccountVerificationRequiredException
import com.didit.application.auth.exception.ExpiredRefreshTokenException
import com.didit.application.auth.exception.InvalidRefreshTokenException
import com.didit.application.auth.exception.UnsupportedOAuthProviderException
import com.didit.application.auth.provided.Auth
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.SocialIdentityRepository
import com.didit.application.auth.required.TokenProvider
import com.didit.application.auth.required.UserRepository
import com.didit.application.auth.required.WithdrawalRecordRepository
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialIdentity
import com.didit.domain.auth.User
import com.didit.domain.auth.WithdrawalReason
import com.didit.domain.auth.WithdrawalRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val socialIdentityRepository: SocialIdentityRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userFinder: UserFinder,
    private val oAuthClientFactory: OAuthClientFactory,
    private val tokenProvider: TokenProvider,
    private val withdrawalRecordRepository: WithdrawalRecordRepository,
    private val auditLogger: AuditLogger,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val loginCompletionService: LoginCompletionService,
    @param:Value("\${social-login.apple-enabled:false}") private val appleEnabled: Boolean,
    transactionManager: PlatformTransactionManager,
) : Auth {
    private val loginTransaction = TransactionTemplate(transactionManager)

    companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun login(
        provider: Provider,
        oauthToken: String,
    ): TokenResponse {
        if (provider == Provider.APPLE && !appleEnabled) throw UnsupportedOAuthProviderException()

        val userInfo = oAuthClientFactory.getClient(provider).getUserInfo(oauthToken)

        return loginTransaction.execute {
            val (user, isNewUser) = resolveUser(provider, userInfo.providerId)
            logger.info("로그인 성공 - userId: ${user.id}, provider: $provider, isNewUser: $isNewUser")
            loginCompletionService.complete(user, provider, isNewUser)
        } ?: error("소셜 로그인 트랜잭션이 결과를 반환하지 않았습니다.")
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
        socialIdentityRepository.deleteAllByUserId(user.id)

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
    ): Pair<User, Boolean> {
        socialIdentityRepository.findByProviderAndProviderId(provider, providerId)?.let { identity ->
            val user = userRepository.findById(identity.userId)
            if (user != null && !user.isDeleted) return user to false
            socialIdentityRepository.delete(identity)
        }

        userRepository.findByProviderAndProviderId(provider, providerId)?.let {
            socialIdentityRepository.save(SocialIdentity.create(it.id, provider, providerId))
            return it to false
        }
        userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(provider, providerId)?.let { withdrawn ->
            if (!withdrawn.isAnonymized) {
                withdrawn.anonymize()
                userRepository.save(withdrawn)
            }
        }
        throw AccountVerificationRequiredException()
    }

    private fun rejoinUser(user: User): User {
        logger.info("재가입 유저 - userId: ${user.id}")

        user.rejoin()

        return userRepository.save(user)
    }
}
