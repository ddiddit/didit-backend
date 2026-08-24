package com.didit.application.auth

import com.didit.application.audit.AuditLogger
import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.AccountVerificationRequiredException
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.OAuthClient
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.SocialIdentityRepository
import com.didit.application.auth.required.TokenProvider
import com.didit.application.auth.required.UserRepository
import com.didit.application.auth.required.WithdrawalRecordRepository
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialIdentity
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var identityRepository: SocialIdentityRepository

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    lateinit var userFinder: UserFinder

    @Mock
    lateinit var tokenProvider: TokenProvider

    @Mock
    lateinit var withdrawalRecordRepository: WithdrawalRecordRepository

    @Mock
    lateinit var auditLogger: AuditLogger

    @Mock
    lateinit var deviceTokenRepository: DeviceTokenRepository

    @Mock
    lateinit var loginCompletionService: LoginCompletionService

    private lateinit var service: AuthService

    @BeforeEach
    fun setUp() {
        val client =
            object : OAuthClient {
                override fun getUserInfo(oauthToken: String) = UserInfo("new-provider-id", "member@example.com")
            }
        service =
            AuthService(
                userRepository = userRepository,
                socialIdentityRepository = identityRepository,
                refreshTokenRepository = refreshTokenRepository,
                userFinder = userFinder,
                oAuthClientFactory = OAuthClientFactory(mapOf(Provider.KAKAO to client)),
                tokenProvider = tokenProvider,
                withdrawalRecordRepository = withdrawalRecordRepository,
                auditLogger = auditLogger,
                deviceTokenRepository = deviceTokenRepository,
                loginCompletionService = loginCompletionService,
            )
    }

    @Test
    fun `v1 로그인은 모르는 식별자를 신규 회원으로 자동 생성하지 않는다`() {
        whenever(identityRepository.findByProviderAndProviderId(Provider.KAKAO, "new-provider-id")).thenReturn(null)
        whenever(userRepository.findByProviderAndProviderId(Provider.KAKAO, "new-provider-id")).thenReturn(null)
        whenever(userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(Provider.KAKAO, "new-provider-id"))
            .thenReturn(null)

        assertThatThrownBy { service.login(Provider.KAKAO, "access-token") }
            .isInstanceOf(AccountVerificationRequiredException::class.java)

        verify(userRepository, never()).save(any())
        verify(loginCompletionService, never()).complete(any(), any(), any())
    }

    @Test
    fun `v1 로그인에서 기존 users 식별자가 발견되면 새 식별자 테이블을 자동 보정한다`() {
        val user = UserFixture.create(provider = Provider.KAKAO, providerId = "new-provider-id")
        whenever(identityRepository.findByProviderAndProviderId(Provider.KAKAO, "new-provider-id")).thenReturn(null)
        whenever(userRepository.findByProviderAndProviderId(Provider.KAKAO, "new-provider-id")).thenReturn(user)

        service.login(Provider.KAKAO, "access-token")

        val identityCaptor = argumentCaptor<SocialIdentity>()
        verify(identityRepository).save(identityCaptor.capture())
        verify(loginCompletionService).complete(user, Provider.KAKAO, false)
    }
}
