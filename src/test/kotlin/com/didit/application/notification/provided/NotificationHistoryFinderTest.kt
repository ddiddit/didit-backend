package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationHistory
import com.didit.support.NotificationHistoryFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationHistoryFinderTest {
    @Mock
    lateinit var notificationHistoryFinder: NotificationHistoryFinder

    @Test
    fun `findAllByUserId`() {
        val userId = UUID.randomUUID()
        val histories =
            listOf(
                NotificationHistory.create(NotificationHistoryFixture.createRequest(userId)),
            )
        whenever(notificationHistoryFinder.findAllByUserId(userId)).thenReturn(histories)

        val found = notificationHistoryFinder.findAllByUserId(userId)

        verify(notificationHistoryFinder).findAllByUserId(userId)
        assertThat(found).hasSize(1)
    }
}
