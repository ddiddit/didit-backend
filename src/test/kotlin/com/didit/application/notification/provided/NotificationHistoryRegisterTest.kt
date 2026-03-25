package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationHistory
import com.didit.support.NotificationHistoryFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationHistoryRegisterTest {
    @Mock
    lateinit var notificationHistoryRegister: NotificationHistoryRegister

    @Test
    fun `save`() {
        val request = NotificationHistoryFixture.createRequest()
        val history = NotificationHistory.create(request)
        whenever(notificationHistoryRegister.save(request)).thenReturn(history)

        notificationHistoryRegister.save(request)

        verify(notificationHistoryRegister).save(request)
    }

    @Test
    fun `readAll`() {
        val userId = UUID.randomUUID()

        notificationHistoryRegister.readAll(userId)

        verify(notificationHistoryRegister).readAll(userId)
    }
}
