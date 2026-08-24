package com.didit.application.auth

import com.didit.application.auth.dto.SocialLoginStatus
import com.didit.application.auth.dto.TokenResponse
import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.required.EmailVerificationChallengeRepository
import com.didit.application.auth.required.OAuthClient
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.application.auth.required.SocialIdentityRepository
import com.didit.application.auth.required.SocialLoginSessionRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.EmailSender
import com.didit.domain.auth.EmailVerificationChallenge
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType
import com.didit.domain.auth.SocialIdentity
import com.didit.domain.auth.SocialLoginSession
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class SocialAuthServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var identityRepository: SocialIdentityRepository

    @Mock
    lateinit var sessionRepository: SocialLoginSessionRepository

    @Mock
    lateinit var challengeRepository: EmailVerificationChallengeRepository

    @Mock
    lateinit var challengeManager: EmailVerificationChallengeManager

    @Mock
    lateinit var loginCompletionService: LoginCompletionService

    @Mock
    lateinit var emailSender: EmailSender

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var service: SocialAuthService

    @BeforeEach
    fun setUp() {
        val kakaoClient =
            object : OAuthClient {
                override fun getUserInfo(oauthToken: String) = UserInfo("new-kakao-id", "member@example.com")

                override fun getUserInfo(
                    credentialType: SocialCredentialType,
                    credential: String,
                ) = getUserInfo(credential)
            }

        service =
            SocialAuthService(
                userRepository = userRepository,
                identityRepository = identityRepository,
                sessionRepository = sessionRepository,
                challengeRepository = challengeRepository,
                oAuthClientFactory = OAuthClientFactory(mapOf(Provider.KAKAO to kakaoClient)),
                challengeManager = challengeManager,
                loginCompletionService = loginCompletionService,
                emailSender = emailSender,
                passwordEncoder = passwordEncoder,
                sessionExpiryMinutes = 15,
                maxAttempts = 5,
                appleEnabled = false,
            )
    }

    @Test
    fun `등록된 소셜 식별자는 이메일 인증 없이 로그인한다`() {
        val user = UserFixture.create(provider = Provider.KAKAO, providerId = "old-kakao-id")
        val identity = SocialIdentity.create(user.id, Provider.KAKAO, "new-kakao-id")
        whenever(identityRepository.findByProviderAndProviderId(Provider.KAKAO, "new-kakao-id")).thenReturn(identity)
        whenever(userRepository.findById(user.id)).thenReturn(user)
        whenever(loginCompletionService.complete(user, Provider.KAKAO, false)).thenReturn(tokenResponse(isNewUser = false))

        val result = service.login(Provider.KAKAO, SocialCredentialType.AUTHORIZATION_CODE, "authorization-code")

        assertThat(result.status).isEqualTo(SocialLoginStatus.AUTHENTICATED)
        assertThat(result.token?.isNewUser).isFalse()
        verify(sessionRepository, never()).save(any())
    }

    @Test
    fun `모르는 소셜 식별자는 자동가입하지 않고 이메일 인증 세션을 발급한다`() {
        whenever(identityRepository.findByProviderAndProviderId(Provider.KAKAO, "new-kakao-id")).thenReturn(null)
        whenever(userRepository.findByProviderAndProviderId(Provider.KAKAO, "new-kakao-id")).thenReturn(null)
        whenever(sessionRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.login(Provider.KAKAO, SocialCredentialType.AUTHORIZATION_CODE, "authorization-code")

        assertThat(result.status).isEqualTo(SocialLoginStatus.EMAIL_VERIFICATION_REQUIRED)
        assertThat(result.loginSessionToken).isNotBlank()
        assertThat(result.emailHint).isEqualTo("m***@example.com")
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `OTP 인증 이메일과 같은 제공자의 기존 회원 한 명이면 기존 계정에 연결한다`() {
        val session = validSession()
        val challenge = validChallenge(session)
        val existingUser = UserFixture.create(provider = Provider.KAKAO, providerId = "old-kakao-id", email = "member@example.com")
        prepareSuccessfulVerification(session, challenge)
        whenever(userRepository.findAllActiveByNormalizedEmail("member@example.com")).thenReturn(listOf(existingUser))
        whenever(loginCompletionService.complete(existingUser, Provider.KAKAO, false)).thenReturn(tokenResponse(isNewUser = false))

        val result = service.verifyEmail("raw-session-token", "123456")

        assertThat(result.status).isEqualTo(SocialLoginStatus.AUTHENTICATED)
        assertThat(result.token?.isNewUser).isFalse()
        val identityCaptor = argumentCaptor<SocialIdentity>()
        verify(identityRepository).save(identityCaptor.capture())
        assertThat(identityCaptor.firstValue.userId).isEqualTo(existingUser.id)
        assertThat(identityCaptor.firstValue.providerId).isEqualTo("new-kakao-id")
        assertThat(session.isConsumed).isTrue()
        assertThat(challenge.isVerified).isTrue()
    }

    @Test
    fun `같은 이메일의 다른 제공자 회원은 자동 병합하지 않고 고객지원 대상으로 판정한다`() {
        val session = validSession()
        val challenge = validChallenge(session)
        val googleUser = UserFixture.create(provider = Provider.GOOGLE, providerId = "google-id", email = "member@example.com")
        prepareSuccessfulVerification(session, challenge)
        whenever(userRepository.findAllActiveByNormalizedEmail("member@example.com")).thenReturn(listOf(googleUser))

        val result = service.verifyEmail("raw-session-token", "123456")

        assertThat(result.status).isEqualTo(SocialLoginStatus.SUPPORT_REQUIRED)
        verify(identityRepository, never()).save(any())
        verify(loginCompletionService, never()).complete(any(), any(), any())
        assertThat(session.isConsumed).isTrue()
    }

    @Test
    fun `일치하는 이메일 회원이 없을 때만 새 회원을 생성한다`() {
        val session = validSession()
        val challenge = validChallenge(session)
        prepareSuccessfulVerification(session, challenge)
        whenever(userRepository.findAllActiveByNormalizedEmail("member@example.com")).thenReturn(emptyList())
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(loginCompletionService.complete(any(), any(), any())).thenReturn(tokenResponse(isNewUser = true))

        val result = service.verifyEmail("raw-session-token", "123456")

        assertThat(result.status).isEqualTo(SocialLoginStatus.AUTHENTICATED)
        assertThat(result.token?.isNewUser).isTrue()
        verify(userRepository).save(any())
        verify(identityRepository).save(any())
    }

    private fun prepareSuccessfulVerification(
        session: SocialLoginSession,
        challenge: EmailVerificationChallenge,
    ) {
        whenever(sessionRepository.findByTokenHashForUpdate(any())).thenReturn(session)
        whenever(challengeRepository.findActiveBySessionIdForUpdate(session.id)).thenReturn(listOf(challenge))
        whenever(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true)
        whenever(identityRepository.findByProviderAndProviderId(Provider.KAKAO, "new-kakao-id")).thenReturn(null)
        whenever(userRepository.findByProviderAndProviderId(Provider.KAKAO, "new-kakao-id")).thenReturn(null)
    }

    private fun validSession() =
        SocialLoginSession(
            tokenHash = SocialTokenCodec.hash("raw-session-token"),
            provider = Provider.KAKAO,
            providerId = "new-kakao-id",
            providerEmail = "member@example.com",
            expiresAt = LocalDateTime.now().plusMinutes(10),
        )

    private fun validChallenge(session: SocialLoginSession) =
        EmailVerificationChallenge(
            sessionId = session.id,
            email = "member@example.com",
            otpHash = "encoded-code",
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )

    private fun tokenResponse(isNewUser: Boolean) =
        TokenResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            isNewUser = isNewUser,
            isOnboardingCompleted = false,
        )
}
