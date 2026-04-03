package com.didit.application.notice.provided

import com.didit.domain.notice.NoticeRegisterRequest
import com.didit.domain.notice.NoticeStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NoticeModifierTest {
    @Mock
    lateinit var noticeModifier: NoticeModifier

    @Test
    fun `modify()`() {
        val noticeId = UUID.randomUUID()
        val adminId = UUID.randomUUID()

        val request =
            NoticeRegisterRequest(
                title = "제목",
                content = "내용",
                status = NoticeStatus.PUBLISHED,
                sendPush = true,
            )

        noticeModifier.modify(request, noticeId, adminId)

        verify(noticeModifier).modify(request, noticeId, adminId)
    }

    @Test
    fun `delete()`() {
        val noticeId = UUID.randomUUID()
        val adminId = UUID.randomUUID()

        noticeModifier.delete(noticeId, adminId)

        verify(noticeModifier).delete(noticeId, adminId)
    }
}
