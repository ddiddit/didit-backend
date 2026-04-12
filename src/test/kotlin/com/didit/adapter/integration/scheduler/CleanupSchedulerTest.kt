package com.didit.adapter.integration.scheduler

import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.application.notification.required.NotificationSettingRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import com.didit.support.UserFixture
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class CleanupSchedulerTest {
    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var notificationHistoryRepository: NotificationHistoryRepository

    @Mock
    lateinit var notificationSettingRepository: NotificationSettingRepository

    @InjectMocks
    lateinit var cleanupScheduler: CleanupScheduler

    @Test
    fun `PENDING 회고가 있으면 삭제된다`() {
        val retro1 = Retrospective.create(UUID.randomUUID())
        val retro2 = Retrospective.create(UUID.randomUUID())
        whenever(retrospectiveRepository.findAllPendingBefore(any())).thenReturn(listOf(retro1, retro2))
        whenever(refreshTokenRepository.deleteAllExpiredBefore(any())).thenReturn(0)
        whenever(userRepository.findAllWithdrawnBefore(any())).thenReturn(emptyList())

        cleanupScheduler.cleanup()

        verify(retrospectiveRepository).delete(retro1)
        verify(retrospectiveRepository).delete(retro2)
    }

    @Test
    fun `탈퇴 유저의 관련 데이터가 순서대로 삭제된다`() {
        val user = UserFixture.create().apply { withdraw() }
        whenever(retrospectiveRepository.findAllPendingBefore(any())).thenReturn(emptyList())
        whenever(refreshTokenRepository.deleteAllExpiredBefore(any())).thenReturn(0)
        whenever(userRepository.findAllWithdrawnBefore(any())).thenReturn(listOf(user))
        whenever(retrospectiveRepository.findAllByUserId(user.id)).thenReturn(emptyList())

        cleanupScheduler.cleanup()

        val inOrder = inOrder(notificationHistoryRepository, notificationSettingRepository, userRepository)

        inOrder.verify(notificationHistoryRepository).deleteAllByUserId(user.id)
        inOrder.verify(notificationSettingRepository).deleteByUserId(user.id)
        inOrder.verify(userRepository).delete(user)
    }

    @Test
    fun `만료된 리프레시 토큰이 삭제된다`() {
        whenever(retrospectiveRepository.findAllPendingBefore(any())).thenReturn(emptyList())
        whenever(refreshTokenRepository.deleteAllExpiredBefore(any())).thenReturn(3)
        whenever(userRepository.findAllWithdrawnBefore(any())).thenReturn(emptyList())

        cleanupScheduler.cleanup()

        verify(refreshTokenRepository).deleteAllExpiredBefore(any())
    }
}
