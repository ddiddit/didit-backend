package com.didit.adapter.integration.scheduler

import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
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

    @InjectMocks
    lateinit var cleanupScheduler: CleanupScheduler

    @Test
    fun `PENDING 회고가 있으면 삭제된다`() {
        val retro1 = Retrospective.create(UUID.randomUUID())
        val retro2 = Retrospective.create(UUID.randomUUID())
        whenever(retrospectiveRepository.findAllPendingBefore(any())).thenReturn(listOf(retro1, retro2))

        cleanupScheduler.cleanup()

        verify(retrospectiveRepository).delete(retro1)
        verify(retrospectiveRepository).delete(retro2)
    }

    @Test
    fun `PENDING 회고가 없으면 삭제하지 않는다`() {
        whenever(retrospectiveRepository.findAllPendingBefore(any())).thenReturn(emptyList())

        cleanupScheduler.cleanup()

        verify(retrospectiveRepository, never()).delete(any())
    }

    @Test
    fun `만료된 리프레시 토큰이 삭제된다`() {
        whenever(retrospectiveRepository.findAllPendingBefore(any())).thenReturn(emptyList())
        whenever(refreshTokenRepository.deleteAllExpiredBefore(any())).thenReturn(3)

        cleanupScheduler.cleanup()

        verify(refreshTokenRepository).deleteAllExpiredBefore(any())
    }
}
