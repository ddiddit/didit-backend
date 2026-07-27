package com.didit.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.util.UUID

class AdminNoticePushTest {
    @Test
    fun `success - create all target request`() {
        val request =
            AdminNoticePushSendRequest(
                adminId = UUID.randomUUID(),
                targetType = AdminNoticePushTargetType.ALL,
                userIds = emptyList(),
                title = "새로운 소식",
                body = "디딧의 새로운 기능을 확인해 보세요.",
                link = "/notices/1",
            )

        assertThat(request.targetType).isEqualTo(AdminNoticePushTargetType.ALL)
    }

    @Test
    fun `fail - all target request has user ids`() {
        assertThatIllegalArgumentException().isThrownBy {
            AdminNoticePushSendRequest(
                adminId = UUID.randomUUID(),
                targetType = AdminNoticePushTargetType.ALL,
                userIds = listOf(UUID.randomUUID()),
                title = "제목",
                body = "본문",
                link = "/notices/1",
            )
        }
    }

    @Test
    fun `fail - selected target request has no user ids`() {
        assertThatIllegalArgumentException().isThrownBy {
            AdminNoticePushSendRequest(
                adminId = UUID.randomUUID(),
                targetType = AdminNoticePushTargetType.SELECTED_USERS,
                userIds = emptyList(),
                title = "제목",
                body = "본문",
                link = "/notices/1",
            )
        }
    }

    @Test
    fun `fail - title body or link is blank`() {
        listOf(
            Triple("", "본문", "/notices/1"),
            Triple("제목", "", "/notices/1"),
            Triple("제목", "본문", ""),
        ).forEach { (title, body, link) ->
            assertThatIllegalArgumentException().isThrownBy {
                AdminNoticePushSendRequest(
                    adminId = UUID.randomUUID(),
                    targetType = AdminNoticePushTargetType.ALL,
                    userIds = emptyList(),
                    title = title,
                    body = body,
                    link = link,
                )
            }
        }
    }
}
