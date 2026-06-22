package com.didit.application.notification

import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.UserPushSender
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetrospectiveNotificationEventListenerTest {
    @Mock
    lateinit var notificationHistoryRegister: NotificationHistoryRegister

    @Mock
    lateinit var userPushSender: UserPushSender

    private lateinit var listener: RetrospectiveNotificationEventListener

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        listener = RetrospectiveNotificationEventListener(notificationHistoryRegister, userPushSender)
    }

    @Test
    fun `onRetrospectiveCompleted - 이벤트 수신 시 회고 결과 생성 완료 알림을 저장한다`() {
        val userId = UUID.randomUUID()
        val retrospectiveId = UUID.randomUUID()
        val expectedLink = "/retrospects/$retrospectiveId"
        val event = RetrospectiveCompletedEvent(userId = userId, retrospectiveId = retrospectiveId, retroDate = LocalDate.now())

        listener.onRetrospectiveCompleted(event)

        verify(notificationHistoryRegister).save(
            argThat<NotificationHistoryCreateRequest> {
                this.userId == userId &&
                    this.type == NotificationType.RETROSPECTIVE_RESULT_CREATED &&
                    this.title == RetrospectiveNotificationEventListener.RETROSPECTIVE_RESULT_CREATED_TITLE &&
                    this.body == RetrospectiveNotificationEventListener.RETROSPECTIVE_RESULT_CREATED_BODY &&
                    this.link == expectedLink
            },
        )
        verify(userPushSender).sendToUser(
            eq(userId),
            eq(RetrospectiveNotificationEventListener.RETROSPECTIVE_RESULT_CREATED_TITLE),
            eq(RetrospectiveNotificationEventListener.RETROSPECTIVE_RESULT_CREATED_BODY),
            eq(expectedLink),
        )
    }

    @Test
    fun `onRetrospectiveCompleted - 알림 저장 실패 시 예외를 전파하지 않는다`() {
        val event =
            RetrospectiveCompletedEvent(userId = UUID.randomUUID(), retrospectiveId = UUID.randomUUID(), retroDate = LocalDate.now())
        whenever(notificationHistoryRegister.save(any())).thenThrow(RuntimeException("알림 저장 실패"))

        assertDoesNotThrow {
            listener.onRetrospectiveCompleted(event)
        }

        verify(notificationHistoryRegister).save(any())
    }
}
