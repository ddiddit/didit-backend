package com.didit.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class AdminNoticeEmailTest {
    private val adminId = UUID.randomUUID()

    @Test
    fun `success - 전체 발송 요청 생성`() {
        val request =
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.ALL,
                userIds = emptyList(),
                subject = "공지",
                body = "내용",
            )

        assertThat(request.targetType).isEqualTo(AdminNoticeEmailTargetType.ALL)
        assertThat(request.userIds).isEmpty()
    }

    @Test
    fun `success - 선택 발송 요청 생성`() {
        val userId = UUID.randomUUID()

        val request =
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.SELECTED_USERS,
                userIds = listOf(userId),
                subject = "공지",
                body = "내용",
            )

        assertThat(request.targetType).isEqualTo(AdminNoticeEmailTargetType.SELECTED_USERS)
        assertThat(request.userIds).containsExactly(userId)
    }

    @Test
    fun `fail - 제목이 비어있다`() {
        assertThatThrownBy {
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.ALL,
                userIds = emptyList(),
                subject = "",
                body = "내용",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `fail - 본문이 비어있다`() {
        assertThatThrownBy {
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.ALL,
                userIds = emptyList(),
                subject = "공지",
                body = "",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `fail - 전체 발송 시 userId가 존재한다`() {
        assertThatThrownBy {
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.ALL,
                userIds = listOf(UUID.randomUUID()),
                subject = "공지",
                body = "내용",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `fail - 선택 발송 시 userIds가 비어있다`() {
        assertThatThrownBy {
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = AdminNoticeEmailTargetType.SELECTED_USERS,
                userIds = emptyList(),
                subject = "공지",
                body = "내용",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}