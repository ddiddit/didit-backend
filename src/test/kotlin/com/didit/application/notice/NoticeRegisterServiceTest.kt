package com.didit.application.notice

import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.NoticeRegisterRequest
import com.didit.domain.notice.NoticeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class NoticeRegisterServiceTest {
    @Mock
    lateinit var noticeRepository: NoticeRepository

    @InjectMocks
    lateinit var noticeRegisterService: NoticeRegisterService

    private val adminId = UUID.randomUUID()

    @Test
    fun `공지 등록 성공`() {
        val request =
            NoticeRegisterRequest(
                title = "공지 제목",
                content = "공지 내용",
                status = NoticeStatus.PUBLISHED,
                sendPush = true,
            )

        whenever(noticeRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = noticeRegisterService.register(request, adminId)

        verify(noticeRepository).save(any())

        assertThat(result.title).isEqualTo(request.title)
        assertThat(result.content).isEqualTo(request.content)
        assertThat(result.status).isEqualTo(request.status)
        assertThat(result.sendPush).isEqualTo(request.sendPush)
        assertThat(result.adminId).isEqualTo(adminId)
    }
}
