package com.didit.application.notification

import com.didit.application.audit.AuditLogger
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.EmailSender
import com.didit.domain.notification.AdminNoticeEmailSendRequest
import com.didit.domain.notification.AdminNoticeEmailTargetType
import com.didit.support.UserFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminNoticeEmailServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var emailSender: EmailSender

    @Mock
    lateinit var auditLogger: AuditLogger

    @InjectMocks
    lateinit var service: AdminNoticeEmailService

    @Test
    fun `전체 사용자에게 공지 이메일 발송`() {
        val adminId = UUID.randomUUID()
        val user1 = UserFixture.create(email = "a@test.com")
        val user2 = UserFixture.create(providerId = "kakao-2", email = "b@test.com")

        whenever(userRepository.findAllByDeletedAtIsNullAndEmailIsNotNull())
            .thenReturn(listOf(user1, user2))

        service.send(
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.ALL,
                userIds = emptyList(),
                subject = "공지 제목",
                body = "<p>공지 내용</p>",
            ),
        )

        verify(emailSender).send("a@test.com", "공지 제목", "<p>공지 내용</p>")
        verify(emailSender).send("b@test.com", "공지 제목", "<p>공지 내용</p>")
    }

    @Test
    fun `선택 사용자에게 공지 이메일 발송`() {
        val adminId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val user = UserFixture.create(email = "selected@test.com")

        whenever(userRepository.findAllByIdInAndDeletedAtIsNullAndEmailIsNotNull(listOf(userId)))
            .thenReturn(listOf(user))

        service.send(
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.SELECTED_USERS,
                userIds = listOf(userId),
                subject = "선택 공지",
                body = "<p>선택 내용</p>",
            ),
        )

        verify(emailSender).send("selected@test.com", "선택 공지", "<p>선택 내용</p>")
    }

    @Test
    fun `일부 이메일 발송 실패 시 나머지는 계속 발송`() {
        val adminId = UUID.randomUUID()
        val user1 = UserFixture.create(email = "fail@test.com")
        val user2 = UserFixture.create(providerId = "kakao-2", email = "success@test.com")

        whenever(userRepository.findAllByDeletedAtIsNullAndEmailIsNotNull())
            .thenReturn(listOf(user1, user2))

        doThrow(RuntimeException("smtp error"))
            .whenever(emailSender)
            .send("fail@test.com", "공지", "<p>내용</p>")

        service.send(
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.ALL,
                userIds = emptyList(),
                subject = "공지",
                body = "<p>내용</p>",
            ),
        )

        verify(emailSender).send("fail@test.com", "공지", "<p>내용</p>")
        verify(emailSender).send("success@test.com", "공지", "<p>내용</p>")
    }
}
