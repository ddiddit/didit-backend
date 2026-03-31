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
class NoticeRegisterTest {
    @Mock
    lateinit var noticeRegister: NoticeRegister

    @Test
    fun `register()`() {
        val adminId = UUID.randomUUID()

        val request =
            NoticeRegisterRequest(
                title = "공지 제목",
                content = "공지 내용",
                status = NoticeStatus.PUBLISHED,
                sendPush = true,
            )

        noticeRegister.register(request, adminId)

        verify(noticeRegister).register(request, adminId)
    }
}
