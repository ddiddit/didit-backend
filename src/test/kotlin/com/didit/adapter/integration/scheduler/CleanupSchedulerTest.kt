package com.didit.adapter.integration.scheduler

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class CleanupSchedulerTest {
    @Mock
    lateinit var cleanupExecutor: CleanupExecutor

    @InjectMocks
    lateinit var cleanupScheduler: CleanupScheduler

    @Test
    fun `cleanup - 각 정리 작업을 실행한다`() {
        whenever(cleanupExecutor.findWithdrawnToAnonymize()).thenReturn(emptyList())
        whenever(cleanupExecutor.findWithdrawnToDelete()).thenReturn(emptyList())

        cleanupScheduler.cleanup()

        verify(cleanupExecutor).cleanPendingRetrospects()
        verify(cleanupExecutor).cleanExpiredRefreshTokens()
        verify(cleanupExecutor).findWithdrawnToAnonymize()
        verify(cleanupExecutor).findWithdrawnToDelete()
    }

    @Test
    fun `deleteWithdrawnUsers - 한 유저 삭제가 실패해도 나머지 유저는 삭제된다`() {
        val failing = UUID.randomUUID()
        val succeeding = UUID.randomUUID()

        whenever(cleanupExecutor.findWithdrawnToAnonymize()).thenReturn(emptyList())
        whenever(cleanupExecutor.findWithdrawnToDelete()).thenReturn(listOf(failing, succeeding))
        doThrow(RuntimeException("삭제 실패")).whenever(cleanupExecutor).deleteUser(failing)

        cleanupScheduler.cleanup()

        verify(cleanupExecutor).deleteUser(failing)
        verify(cleanupExecutor).deleteUser(succeeding)
    }

    @Test
    fun `anonymizeWithdrawnUsers - 한 유저 익명화가 실패해도 나머지 유저는 익명화된다`() {
        val failing = UUID.randomUUID()
        val succeeding = UUID.randomUUID()

        whenever(cleanupExecutor.findWithdrawnToAnonymize()).thenReturn(listOf(failing, succeeding))
        whenever(cleanupExecutor.findWithdrawnToDelete()).thenReturn(emptyList())
        doThrow(RuntimeException("익명화 실패")).whenever(cleanupExecutor).anonymize(failing)

        cleanupScheduler.cleanup()

        verify(cleanupExecutor).anonymize(failing)
        verify(cleanupExecutor).anonymize(succeeding)
    }
}
