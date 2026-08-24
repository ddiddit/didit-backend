package com.didit.application.auth

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.TokenProvider
import com.didit.domain.auth.Provider
import com.didit.domain.auth.RefreshToken
import com.didit.domain.auth.User
import com.didit.domain.auth.UserLoggedInEvent
import com.didit.domain.shared.ServiceTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class LoginCompletionService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
    private val auditLogger: AuditLogger,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun complete(
        user: User,
        provider: Provider,
        isNewUser: Boolean,
    ): TokenResponse {
        val refreshToken = tokenProvider.generateRefreshToken()
        refreshTokenRepository.save(
            RefreshToken.create(user.id, refreshToken, tokenProvider.getRefreshTokenExpiresAt()),
        )

        auditLogger.log(
            actorId = user.id,
            actorType = ActorType.USER,
            action = AuditAction.USER_LOGGED_IN,
            payload = mapOf("provider" to provider.name),
        )
        eventPublisher.publishEvent(UserLoggedInEvent(user.id, ServiceTime.today()))

        return TokenResponse(
            accessToken = tokenProvider.generateAccessToken(user.id),
            refreshToken = refreshToken,
            isNewUser = isNewUser,
            isOnboardingCompleted = user.isOnboardingCompleted,
        )
    }
}
