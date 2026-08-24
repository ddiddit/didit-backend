package com.didit.application.auth

import com.didit.application.auth.exception.EmailRequiredException
import com.didit.application.auth.exception.EmailVerificationResendTooSoonException
import com.didit.application.auth.exception.ExpiredSocialLoginSessionException
import com.didit.application.auth.exception.InvalidSocialLoginSessionException
import com.didit.application.auth.required.EmailVerificationChallengeRepository
import com.didit.application.auth.required.SocialLoginSessionRepository
import com.didit.domain.auth.EmailVerificationChallenge
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

data class CreatedEmailChallenge(
    val email: String,
    val rawCode: String,
    val expiresInSeconds: Long,
)

@Service
class EmailVerificationChallengeManager(
    private val sessionRepository: SocialLoginSessionRepository,
    private val challengeRepository: EmailVerificationChallengeRepository,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${social-login.email-verification.expiry-minutes:10}") private val expiryMinutes: Long,
    @param:Value("\${social-login.email-verification.resend-cooldown-seconds:60}") private val resendCooldownSeconds: Long,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun create(
        loginSessionToken: String,
        requestedEmail: String,
    ): CreatedEmailChallenge {
        val now = LocalDateTime.now()
        val session =
            sessionRepository.findByTokenHashForUpdate(SocialTokenCodec.hash(loginSessionToken))
                ?: throw InvalidSocialLoginSessionException()
        if (session.isConsumed) throw InvalidSocialLoginSessionException()
        if (session.isExpired(now)) throw ExpiredSocialLoginSessionException()

        val email = normalizeEmail(requestedEmail)
        val latestEmailChallenge = challengeRepository.findLatestByEmailForUpdate(email).firstOrNull()
        val latestEmailCreatedAt = latestEmailChallenge?.createdAt
        if (latestEmailCreatedAt != null && now.isBefore(latestEmailCreatedAt.plusSeconds(resendCooldownSeconds))) {
            throw EmailVerificationResendTooSoonException()
        }

        val activeChallenges = challengeRepository.findActiveBySessionIdForUpdate(session.id)
        val latestCreatedAt = activeChallenges.firstOrNull()?.createdAt
        if (latestCreatedAt != null && now.isBefore(latestCreatedAt.plusSeconds(resendCooldownSeconds))) {
            throw EmailVerificationResendTooSoonException()
        }
        activeChallenges.forEach {
            it.invalidate(now)
            challengeRepository.save(it)
        }

        val rawCode = "%06d".format(secureRandom.nextInt(1_000_000))
        val expiresAt = now.plusMinutes(expiryMinutes)
        challengeRepository.save(
            EmailVerificationChallenge(
                sessionId = session.id,
                email = email,
                otpHash = passwordEncoder.encode(rawCode),
                expiresAt = expiresAt,
            ),
        )

        return CreatedEmailChallenge(
            email = email,
            rawCode = rawCode,
            expiresInSeconds = Duration.between(now, expiresAt).seconds,
        )
    }

    private fun normalizeEmail(email: String): String {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || !EMAIL_REGEX.matches(normalized)) throw EmailRequiredException()
        return normalized
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}
