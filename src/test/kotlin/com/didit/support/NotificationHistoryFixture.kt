package com.didit.support

import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import java.util.UUID

object NotificationHistoryFixture {
    const val TITLE = "회고 작성 알림"
    const val BODY = "하루를 마무리하며 오늘의 회고를 기록해 보세요."

    fun createRequest(userId: UUID = UUID.randomUUID()) =
        NotificationHistoryCreateRequest(
            userId = userId,
            type = NotificationType.DAILY_REMINDER,
            title = TITLE,
            body = BODY,
        )
}
