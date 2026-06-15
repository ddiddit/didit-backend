package com.didit.application.admin

import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditEntry
import com.didit.application.audit.AuditReader
import com.didit.application.auth.exception.UserNotFoundException
import com.didit.application.auth.required.UserRepository
import com.didit.domain.shared.Job
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminUserQueryServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var auditReader: AuditReader

    @InjectMocks
    lateinit var adminUserQueryService: AdminUserQueryService

    private val userId = UUID.randomUUID()

    @Test
    fun `유저 목록 조회 - 마지막 로그인 정보가 매핑된다`() {
        val user = UserFixture.createOnboarded()
        val lastLoginAt = LocalDateTime.now().minusDays(1)
        val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)

        whenever(userRepository.findUsersForAdmin(null, null, null, PageRequest.of(0, 20)))
            .thenReturn(page)
        whenever(auditReader.findLastLoginsByUserIds(any()))
            .thenReturn(mapOf(user.id to lastLoginAt))

        val result = adminUserQueryService.findUsers(null, null, null, 0)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].lastLoginAt).isEqualTo(lastLoginAt)
    }

    @Test
    fun `유저 목록 조회 - 로그인 기록 없는 유저는 lastLoginAt이 null이다`() {
        val user = UserFixture.createOnboarded()
        val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)

        whenever(userRepository.findUsersForAdmin(null, null, null, PageRequest.of(0, 20)))
            .thenReturn(page)
        whenever(auditReader.findLastLoginsByUserIds(any()))
            .thenReturn(emptyMap())

        val result = adminUserQueryService.findUsers(null, null, null, 0)

        assertThat(result.content[0].lastLoginAt).isNull()
    }

    @Test
    fun `유저 상세 조회 성공`() {
        val user = UserFixture.createOnboarded()
        val lastLoginAt = LocalDateTime.now()
        val timeline = listOf(
            AuditEntry(AuditAction.USER_LOGGED_IN, null, LocalDateTime.now()),
        )

        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(auditReader.findLastLogin(userId)).thenReturn(lastLoginAt)
        whenever(auditReader.findTimeline(eq(userId), any(), eq(20))).thenReturn(timeline)

        val result = adminUserQueryService.findUserDetail(userId)

        assertThat(result.profile.onboardingCompleted).isTrue()
        assertThat(result.profile.lastLoginAt).isEqualTo(lastLoginAt)
        assertThat(result.timeline).hasSize(1)
        assertThat(result.timeline[0].action).isEqualTo(AuditAction.USER_LOGGED_IN)
    }

    @Test
    fun `유저 상세 조회 실패 - 존재하지 않는 유저`() {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertThrows<UserNotFoundException> {
            adminUserQueryService.findUserDetail(userId)
        }
    }

    @Test
    fun `키워드 필터로 유저 검색`() {
        val keyword = "test"
        val user = UserFixture.createOnboarded(email = "test@example.com")
        val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)

        whenever(userRepository.findUsersForAdmin(eq(keyword), eq(null), eq(null), any()))
            .thenReturn(page)
        whenever(auditReader.findLastLoginsByUserIds(any())).thenReturn(emptyMap())

        val result = adminUserQueryService.findUsers(keyword, null, null, 0)

        assertThat(result.content).hasSize(1)
    }

    @Test
    fun `직무 필터로 유저 검색`() {
        val user = UserFixture.createOnboarded(job = Job.DEVELOPER)
        val page = PageImpl(listOf(user), PageRequest.of(0, 20), 1)

        whenever(userRepository.findUsersForAdmin(eq(null), eq(Job.DEVELOPER), eq(null), any()))
            .thenReturn(page)
        whenever(auditReader.findLastLoginsByUserIds(any())).thenReturn(emptyMap())

        val result = adminUserQueryService.findUsers(null, Job.DEVELOPER, null, 0)

        assertThat(result.content[0].job).isEqualTo(Job.DEVELOPER)
    }
}
