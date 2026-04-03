package com.didit.domain.notification

import com.didit.support.NotificationHistoryFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class NotificationHistoryTest {
    @Test
    fun `create`() {
        val userId = UUID.randomUUID()
        val history = NotificationHistory.create(NotificationHistoryFixture.createRequest(userId))

        assertThat(history.userId).isEqualTo(userId)
        assertThat(history.type).isEqualTo(NotificationType.DAILY_REMINDER)
        assertThat(history.isRead).isFalse()
    }

    @Test
    fun `read`() {
        val history = NotificationHistory.create(NotificationHistoryFixture.createRequest())

        history.read()

        assertThat(history.isRead).isTrue()
    }

    @Test
    fun `RETENTION_DAYS는 30일이다`() {
        assertThat(NotificationHistory.RETENTION_DAYS).isEqualTo(30L)
    }
}
