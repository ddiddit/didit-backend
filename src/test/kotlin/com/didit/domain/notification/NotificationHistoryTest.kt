package com.didit.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class NotificationHistoryTest {
    @Test
    fun `create`() {
        val userId = UUID.randomUUID()
        val request =
            NotificationHistoryCreateRequest(
                userId = userId,
                type = NotificationType.DAILY_REMINDER,
                title = "회고 작성 알림",
                body = "하루를 마무리하며 오늘의 회고를 기록해 보세요.",
            )
        val history = NotificationHistory.create(request)

        assertThat(history.userId).isEqualTo(userId)
        assertThat(history.type).isEqualTo(NotificationType.DAILY_REMINDER)
        assertThat(history.isRead).isFalse()
    }

    @Test
    fun `read`() {
        val history =
            NotificationHistory.create(
                NotificationHistoryCreateRequest(
                    userId = UUID.randomUUID(),
                    type = NotificationType.DAILY_REMINDER,
                    title = "회고 작성 알림",
                    body = "하루를 마무리하며 오늘의 회고를 기록해 보세요.",
                ),
            )

        history.read()

        assertThat(history.isRead).isTrue()
    }
}
