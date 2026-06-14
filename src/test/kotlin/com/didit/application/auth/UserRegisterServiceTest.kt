package com.didit.application.auth

import com.didit.application.audit.AuditLogger
import com.didit.application.auth.exception.DuplicateNicknameException
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserRegisterServiceTest {
    @Mock
    lateinit var userFinder: UserFinder

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var notificationSettingModifier: NotificationSettingModifier

    @Mock
    lateinit var auditLogger: AuditLogger

    private lateinit var userRegisterService: UserRegisterService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userRegisterService =
            UserRegisterService(
                userFinder = userFinder,
                userRepository = userRepository,
                notificationSettingModifier = notificationSettingModifier,
                auditLogger = auditLogger,
            )
    }

    @Test
    fun `신규 사용자 온보딩 - age와 experience 포함`() {
        val user =
            User(
                id = userId,
                provider = Provider.KAKAO,
                providerId = "kakao123",
            )

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndDeletedAtIsNull("새유저")).thenReturn(false)

        userRegisterService.registerV2(
            userId = userId,
            nickname = "새유저",
            job = Job.DEVELOPER,
            age = UserAge.AGE_30,
            experience = UserExperience.YEARS_3_TO_5,
            marketingAgreed = true,
            nightPushAgreed = false,
        )

        assertThat(user.nickname).isEqualTo("새유저")
        assertThat(user.job).isEqualTo(Job.DEVELOPER)
        assertThat(user.age).isEqualTo(UserAge.AGE_30)
        assertThat(user.experience).isEqualTo(UserExperience.YEARS_3_TO_5)
        assertThat(user.isOnboardingCompleted).isTrue
        verify(userRepository).save(user)
        verify(notificationSettingModifier).updateNightPushConsent(userId, false)
    }

    @Test
    fun `신규 사용자 온보딩 - age와 experience 없이`() {
        val user =
            User(
                id = userId,
                provider = Provider.GOOGLE,
                providerId = "google123",
            )

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndDeletedAtIsNull("새유저")).thenReturn(false)

        userRegisterService.registerV2(
            userId = userId,
            nickname = "새유저",
            job = Job.DESIGNER,
            age = null,
            experience = null,
            marketingAgreed = false,
            nightPushAgreed = true,
        )

        assertThat(user.nickname).isEqualTo("새유저")
        assertThat(user.job).isEqualTo(Job.DESIGNER)
        assertThat(user.age).isNull()
        assertThat(user.experience).isNull()
        assertThat(user.isOnboardingCompleted).isTrue
        verify(userRepository).save(user)
        verify(notificationSettingModifier).updateNightPushConsent(userId, true)
    }

    @Test
    fun `신규 사용자 온보딩 - 닉네임 중복 시 예외 발생`() {
        val user =
            User(
                id = userId,
                provider = Provider.APPLE,
                providerId = "apple123",
            )

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndDeletedAtIsNull("중복닉네임")).thenReturn(true)

        assertThrows<DuplicateNicknameException> {
            userRegisterService.registerV2(
                userId = userId,
                nickname = "중복닉네임",
                job = Job.PLANNER,
                age = null,
                experience = null,
                marketingAgreed = true,
                nightPushAgreed = false,
            )
        }
    }

    @Test
    fun `기존 사용자 프로필 업데이트 - 모든 필드`() {
        val user =
            User(
                id = userId,
                nickname = "기존유저",
                job = Job.DEVELOPER,
                provider = Provider.KAKAO,
                providerId = "kakao456",
            ).apply {
                completeOnboarding(nickname = "기존유저", job = Job.DEVELOPER)
            }

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("수정된닉네임", userId)).thenReturn(false)

        userRegisterService.registerV2(
            userId = userId,
            nickname = "수정된닉네임",
            job = Job.PLANNER,
            age = UserAge.AGE_40_PLUS,
            experience = UserExperience.YEARS_10_PLUS,
            marketingAgreed = true,
            nightPushAgreed = false,
        )

        assertThat(user.nickname).isEqualTo("수정된닉네임")
        assertThat(user.job).isEqualTo(Job.PLANNER)
        assertThat(user.age).isEqualTo(UserAge.AGE_40_PLUS)
        assertThat(user.experience).isEqualTo(UserExperience.YEARS_10_PLUS)
        verify(userRepository).save(user)
    }

    @Test
    fun `기존 사용자 프로필 업데이트 - age와 experience만`() {
        val user =
            User(
                id = userId,
                nickname = "기존유저",
                job = Job.DESIGNER,
                provider = Provider.GOOGLE,
                providerId = "google456",
            ).apply {
                completeOnboarding(nickname = "기존유저", job = Job.DESIGNER)
            }

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("기존유저", userId)).thenReturn(false)

        userRegisterService.registerV2(
            userId = userId,
            nickname = "기존유저",
            job = Job.DESIGNER,
            age = UserAge.AGE_20,
            experience = UserExperience.YEARS_1_TO_2,
            marketingAgreed = true,
            nightPushAgreed = false,
        )

        assertThat(user.nickname).isEqualTo("기존유저")
        assertThat(user.job).isEqualTo(Job.DESIGNER)
        assertThat(user.age).isEqualTo(UserAge.AGE_20)
        assertThat(user.experience).isEqualTo(UserExperience.YEARS_1_TO_2)
        verify(userRepository).save(user)
    }

    @Test
    fun `기존 사용자 프로필 업데이트 - 다른 사용자 닉네임으로 변경 시 예외 발생`() {
        val user =
            User(
                id = userId,
                nickname = "기존유저",
                job = Job.DEVELOPER,
                provider = Provider.APPLE,
                providerId = "apple456",
            ).apply {
                completeOnboarding(nickname = "기존유저", job = Job.DEVELOPER)
            }

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("다른유저", userId)).thenReturn(true)

        assertThrows<DuplicateNicknameException> {
            userRegisterService.registerV2(
                userId = userId,
                nickname = "다른유저",
                job = Job.PLANNER,
                age = UserAge.AGE_30,
                experience = UserExperience.YEARS_3_TO_5,
                marketingAgreed = true,
                nightPushAgreed = false,
            )
        }
    }

    @Test
    fun `기존 사용자 프로필 업데이트 - 자신의 닉네임은 변경 가능`() {
        val user =
            User(
                id = userId,
                nickname = "기존유저",
                job = Job.DEVELOPER,
                provider = Provider.KAKAO,
                providerId = "kakao789",
            ).apply {
                completeOnboarding(nickname = "기존유저", job = Job.DEVELOPER)
            }

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("기존유저", userId)).thenReturn(false)

        userRegisterService.registerV2(
            userId = userId,
            nickname = "기존유저",
            job = Job.DEVELOPER,
            age = UserAge.AGE_30,
            experience = UserExperience.YEARS_3_TO_5,
            marketingAgreed = true,
            nightPushAgreed = false,
        )

        assertThat(user.age).isEqualTo(UserAge.AGE_30)
        assertThat(user.experience).isEqualTo(UserExperience.YEARS_3_TO_5)
        verify(userRepository).save(user)
    }
}
