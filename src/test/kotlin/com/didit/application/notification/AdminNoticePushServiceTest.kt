package com.didit.application.notification

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.UserPushSender
import com.didit.domain.notification.AdminNoticePushSendRequest
import com.didit.domain.notification.AdminNoticePushTargetType
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import com.didit.support.UserFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminNoticePushServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var userPushSender: UserPushSender

    @Mock
    lateinit var notificationHistoryRegister: NotificationHistoryRegister

    @Mock
    lateinit var auditLogger: AuditLogger

    @InjectMocks
    lateinit var service: AdminNoticePushService

    @Test
    fun `send to all marketing agreed users and save histories`() {
        val user1 = UserFixture.create(providerId = "kakao-1")
        val user2 = UserFixture.create(providerId = "kakao-2")
        whenever(userRepository.findAllMarketingAgreed()).thenReturn(listOf(user1, user2))
        val request = createRequest()

        service.send(request)

        listOf(user1, user2).forEach { user ->
            verify(userPushSender).sendToUser(user.id, "마케팅 제목", "마케팅 본문", "/notices/1")
            verify(notificationHistoryRegister).save(
                NotificationHistoryCreateRequest(
                    userId = user.id,
                    type = NotificationType.ADMIN_MARKETING,
                    title = "마케팅 제목",
                    body = "마케팅 본문",
                    link = "/notices/1",
                ),
            )
        }
        verify(auditLogger).log(
            request.adminId,
            ActorType.ADMIN,
            AuditAction.ADMIN_NOTIFICATION_SENT,
            null,
            null,
            mapOf(
                "targetType" to AdminNoticePushTargetType.ALL.name,
                "targetCount" to 2,
                "sentCount" to 2,
                "failedCount" to 0,
            ),
        )
    }

    @Test
    fun `send only selected marketing agreed users`() {
        val userId = UUID.randomUUID()
        val user = UserFixture.create()
        whenever(userRepository.findAllMarketingAgreedByIdIn(listOf(userId))).thenReturn(listOf(user))

        service.send(
            createRequest(
                targetType = AdminNoticePushTargetType.SELECTED_USERS,
                userIds = listOf(userId),
            ),
        )

        verify(userPushSender).sendToUser(user.id, "마케팅 제목", "마케팅 본문", "/notices/1")
    }

    @Test
    fun `continue sending when one user fails and do not save failed history`() {
        val failedUser = UserFixture.create(providerId = "kakao-1")
        val successUser = UserFixture.create(providerId = "kakao-2")
        whenever(userRepository.findAllMarketingAgreed()).thenReturn(listOf(failedUser, successUser))
        doThrow(RuntimeException("fcm error"))
            .whenever(userPushSender)
            .sendToUser(failedUser.id, "마케팅 제목", "마케팅 본문", "/notices/1")

        service.send(createRequest())

        verify(userPushSender).sendToUser(successUser.id, "마케팅 제목", "마케팅 본문", "/notices/1")
        verify(notificationHistoryRegister, never()).save(
            NotificationHistoryCreateRequest(
                userId = failedUser.id,
                type = NotificationType.ADMIN_MARKETING,
                title = "마케팅 제목",
                body = "마케팅 본문",
                link = "/notices/1",
            ),
        )
        verify(notificationHistoryRegister).save(
            NotificationHistoryCreateRequest(
                userId = successUser.id,
                type = NotificationType.ADMIN_MARKETING,
                title = "마케팅 제목",
                body = "마케팅 본문",
                link = "/notices/1",
            ),
        )
    }

    private fun createRequest(
        targetType: AdminNoticePushTargetType = AdminNoticePushTargetType.ALL,
        userIds: List<UUID> = emptyList(),
    ) = AdminNoticePushSendRequest(
        adminId = UUID.randomUUID(),
        targetType = targetType,
        userIds = userIds,
        title = "마케팅 제목",
        body = "마케팅 본문",
        link = "/notices/1",
    )
}
