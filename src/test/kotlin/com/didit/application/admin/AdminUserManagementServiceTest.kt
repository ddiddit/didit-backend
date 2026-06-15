package com.didit.application.admin

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.support.UserFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminUserManagementServiceTest {
    @Mock
    lateinit var userFinder: UserFinder

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    lateinit var deviceTokenRepository: DeviceTokenRepository

    @Mock
    lateinit var auditLogger: AuditLogger

    @InjectMocks
    lateinit var adminUserManagementService: AdminUserManagementService

    private val adminId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    fun `강제 탈퇴 성공 - 유저 상태 변경 및 토큰 삭제`() {
        val user = UserFixture.createOnboarded()

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

        adminUserManagementService.forceWithdraw(adminId, userId)

        assert(user.isDeleted) { "유저가 탈퇴 처리되어야 한다" }

        verify(userRepository).save(user)
        verify(deviceTokenRepository).deleteByUserId(userId)
        verify(refreshTokenRepository).deleteByUserId(userId)
    }

    @Test
    fun `강제 탈퇴 성공 - 감사 로그 기록`() {
        val user = UserFixture.createOnboarded()

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] }

        adminUserManagementService.forceWithdraw(adminId, userId)

        verify(auditLogger).log(
            actorId = eq(adminId),
            actorType = eq(ActorType.ADMIN),
            action = eq(AuditAction.USER_FORCE_WITHDREW),
            targetId = eq(userId),
            targetType = eq("USER"),
        )
    }

    @Test
    fun `강제 탈퇴 실패 - 이미 탈퇴한 유저`() {
        val user = UserFixture.createOnboarded().apply { withdraw() }

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)

        assertThrows<IllegalStateException> {
            adminUserManagementService.forceWithdraw(adminId, userId)
        }
    }
}
