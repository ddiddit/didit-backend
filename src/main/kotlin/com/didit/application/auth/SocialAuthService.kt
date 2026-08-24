package com.didit.application.auth

import com.didit.application.auth.dto.EmailVerificationStartResult
import com.didit.application.auth.dto.SocialLoginResult
import com.didit.application.auth.dto.SocialLoginStatus
import com.didit.application.auth.exception.EmailVerificationAttemptsExceededException
import com.didit.application.auth.exception.ExpiredEmailVerificationException
import com.didit.application.auth.exception.ExpiredSocialLoginSessionException
import com.didit.application.auth.exception.InvalidEmailVerificationException
import com.didit.application.auth.exception.InvalidSocialLoginSessionException
import com.didit.application.auth.exception.UnsupportedOAuthProviderException
import com.didit.application.auth.provided.SocialAuth
import com.didit.application.auth.required.EmailVerificationChallengeRepository
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.application.auth.required.SocialIdentityRepository
import com.didit.application.auth.required.SocialLoginSessionRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.EmailSender
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType
import com.didit.domain.auth.SocialIdentity
import com.didit.domain.auth.SocialLoginSession
import com.didit.domain.auth.User
import com.didit.domain.auth.UserRegisterRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SocialAuthService(
    private val userRepository: UserRepository,
    private val identityRepository: SocialIdentityRepository,
    private val sessionRepository: SocialLoginSessionRepository,
    private val challengeRepository: EmailVerificationChallengeRepository,
    private val oAuthClientFactory: OAuthClientFactory,
    private val challengeManager: EmailVerificationChallengeManager,
    private val loginCompletionService: LoginCompletionService,
    private val emailSender: EmailSender,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${social-login.session-expiry-minutes:15}") private val sessionExpiryMinutes: Long,
    @param:Value("\${social-login.email-verification.max-attempts:5}") private val maxAttempts: Int,
    @param:Value("\${social-login.apple-enabled:false}") private val appleEnabled: Boolean,
) : SocialAuth {
    @Transactional
    override fun login(
        provider: Provider,
        credentialType: SocialCredentialType,
        credential: String,
    ): SocialLoginResult {
        if (provider == Provider.APPLE && !appleEnabled) throw UnsupportedOAuthProviderException()

        val userInfo = oAuthClientFactory.getClient(provider).getUserInfo(credentialType, credential)

        findActiveUser(provider, userInfo.providerId)?.let { user ->
            return authenticated(user, provider, false)
        }

        val rawSessionToken = SocialTokenCodec.generate()
        sessionRepository.save(
            SocialLoginSession(
                tokenHash = SocialTokenCodec.hash(rawSessionToken),
                provider = provider,
                providerId = userInfo.providerId,
                providerEmail = userInfo.email?.trim()?.lowercase(),
                expiresAt = LocalDateTime.now().plusMinutes(sessionExpiryMinutes),
            ),
        )

        return SocialLoginResult(
            status = SocialLoginStatus.EMAIL_VERIFICATION_REQUIRED,
            loginSessionToken = rawSessionToken,
            emailHint = userInfo.email?.let(::maskEmail),
        )
    }

    override fun startEmailVerification(
        loginSessionToken: String,
        email: String,
    ): EmailVerificationStartResult {
        val challenge = challengeManager.create(loginSessionToken, email)

        emailSender.send(
            to = challenge.email,
            subject = "[디딧] 이메일 인증번호 안내",
            body = verificationEmailBody(challenge.rawCode),
        )

        return EmailVerificationStartResult(expiresInSeconds = challenge.expiresInSeconds)
    }

    @Transactional(
        noRollbackFor = [
            InvalidEmailVerificationException::class,
            EmailVerificationAttemptsExceededException::class,
        ],
    )
    override fun verifyEmail(
        loginSessionToken: String,
        code: String,
    ): SocialLoginResult {
        val now = LocalDateTime.now()
        val session =
            sessionRepository.findByTokenHashForUpdate(SocialTokenCodec.hash(loginSessionToken))
                ?: throw InvalidSocialLoginSessionException()
        if (session.isConsumed) throw InvalidSocialLoginSessionException()
        if (session.isExpired(now)) throw ExpiredSocialLoginSessionException()

        val challenge =
            challengeRepository.findActiveBySessionIdForUpdate(session.id).firstOrNull()
                ?: throw InvalidEmailVerificationException()
        if (challenge.isExpired(now)) throw ExpiredEmailVerificationException()
        if (challenge.attemptCount >= maxAttempts) throw EmailVerificationAttemptsExceededException()

        if (!passwordEncoder.matches(code.trim(), challenge.otpHash)) {
            challenge.recordFailure()
            if (challenge.attemptCount >= maxAttempts) {
                challenge.invalidate(now)
                challengeRepository.save(challenge)
                throw EmailVerificationAttemptsExceededException()
            }
            challengeRepository.save(challenge)
            throw InvalidEmailVerificationException()
        }

        val result = resolveVerifiedAccount(session, challenge.email)
        challenge.verify(now)
        session.consume(now)
        challengeRepository.save(challenge)
        sessionRepository.save(session)
        return result
    }

    private fun findActiveUser(
        provider: Provider,
        providerId: String,
    ): User? {
        identityRepository.findByProviderAndProviderId(provider, providerId)?.let { identity ->
            val user = userRepository.findById(identity.userId)
            if (user != null && !user.isDeleted) return user
            identityRepository.delete(identity)
        }

        return userRepository.findByProviderAndProviderId(provider, providerId)?.also { user ->
            identityRepository.save(SocialIdentity.create(user.id, provider, providerId))
        }
    }

    private fun resolveVerifiedAccount(
        session: SocialLoginSession,
        normalizedEmail: String,
    ): SocialLoginResult {
        findActiveUser(session.provider, session.providerId)?.let { user ->
            return authenticated(user, session.provider, false)
        }

        val matchingUsers = userRepository.findAllActiveByNormalizedEmail(normalizedEmail)
        if (matchingUsers.size > 1) return supportRequired()

        val user = matchingUsers.singleOrNull()
        if (user != null && user.provider != session.provider) return supportRequired()

        if (user != null) {
            identityRepository.save(SocialIdentity.create(user.id, session.provider, session.providerId))
            return authenticated(user, session.provider, false)
        }

        val newUser =
            userRepository.save(
                User.register(
                    UserRegisterRequest(
                        provider = session.provider,
                        providerId = session.providerId,
                        email = normalizedEmail,
                    ),
                ),
            )
        identityRepository.save(SocialIdentity.create(newUser.id, session.provider, session.providerId))
        return authenticated(newUser, session.provider, true)
    }

    private fun authenticated(
        user: User,
        provider: Provider,
        isNewUser: Boolean,
    ) = SocialLoginResult(
        status = SocialLoginStatus.AUTHENTICATED,
        token = loginCompletionService.complete(user, provider, isNewUser),
    )

    private fun supportRequired() = SocialLoginResult(status = SocialLoginStatus.SUPPORT_REQUIRED)

    private fun maskEmail(email: String): String {
        val parts = email.split("@", limit = 2)
        if (parts.size != 2) return "***"
        val local = parts[0]
        val visible = local.take(1)
        return "$visible***@${parts[1]}"
    }

    private fun verificationEmailBody(code: String): String =
        """
        <div style="font-family: sans-serif; line-height: 1.6">
          <h2>디딧 이메일 인증</h2>
          <p>아래 인증번호를 로그인 화면에 입력해주세요.</p>
          <p style="font-size: 28px; font-weight: bold; letter-spacing: 6px">$code</p>
          <p>인증번호는 10분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
        </div>
        """.trimIndent()
}
