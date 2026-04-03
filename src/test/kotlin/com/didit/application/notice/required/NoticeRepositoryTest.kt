package com.didit.application.notice.required

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import com.didit.domain.notice.NoticeStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class NoticeRepositoryTest {
    @Mock
    lateinit var noticeRepository: NoticeRepository

    private val adminId = UUID.randomUUID()

    @Test
    fun `save`() {
        val notice = createNotice()

        whenever(noticeRepository.save(notice)).thenReturn(notice)

        val result = noticeRepository.save(notice)

        verify(noticeRepository).save(notice)
        assertEquals(notice, result)
    }

    @Test
    fun `findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc`() {
        noticeRepository.findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(NoticeStatus.PUBLISHED)

        verify(noticeRepository)
            .findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(NoticeStatus.PUBLISHED)
    }

    @Test
    fun `findByIdAndStatusAndDeletedAtIsNull`() {
        val noticeId = UUID.randomUUID()

        noticeRepository.findByIdAndStatusAndDeletedAtIsNull(
            noticeId,
            NoticeStatus.PUBLISHED,
        )

        verify(noticeRepository)
            .findByIdAndStatusAndDeletedAtIsNull(noticeId, NoticeStatus.PUBLISHED)
    }

    @Test
    fun `findAllByDeletedAtIsNullOrderByCreatedAtDesc`() {
        noticeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()

        verify(noticeRepository)
            .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
    }

    @Test
    fun `findByIdAndDeletedAtIsNull`() {
        val noticeId = UUID.randomUUID()

        noticeRepository.findByIdAndDeletedAtIsNull(noticeId)

        verify(noticeRepository)
            .findByIdAndDeletedAtIsNull(noticeId)
    }

    private fun createNotice(): Notice =
        Notice.register(
            NoticeRegisterRequest(
                title = "공지 제목",
                content = "공지 내용",
                status = NoticeStatus.PUBLISHED,
                sendPush = false,
            ),
            adminId,
        )
}
