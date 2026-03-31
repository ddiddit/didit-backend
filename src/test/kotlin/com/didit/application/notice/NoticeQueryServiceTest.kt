package com.didit.application.notice

import com.didit.application.notice.exception.NoticeNotFoundException
import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import com.didit.domain.notice.NoticeStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class NoticeQueryServiceTest {
    @Mock
    lateinit var noticeRepository: NoticeRepository

    @InjectMocks
    lateinit var noticeQueryService: NoticeQueryService

    private val noticeId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()

    @Test
    fun `사용자용 공지 전체 조회 성공`() {
        val notices = listOf(createNotice())

        whenever(
            noticeRepository.findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                NoticeStatus.PUBLISHED,
            ),
        ).thenReturn(notices)

        val result = noticeQueryService.findAll()

        verify(noticeRepository)
            .findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(NoticeStatus.PUBLISHED)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `사용자용 공지 단건 조회 성공`() {
        val notice = createNotice()

        whenever(
            noticeRepository.findByIdAndStatusAndDeletedAtIsNull(
                noticeId,
                NoticeStatus.PUBLISHED,
            ),
        ).thenReturn(notice)

        val result = noticeQueryService.findById(noticeId)

        verify(noticeRepository)
            .findByIdAndStatusAndDeletedAtIsNull(noticeId, NoticeStatus.PUBLISHED)

        assertThat(result).isEqualTo(notice)
    }

    @Test
    fun `사용자용 공지 단건 조회 실패 - 공지 없음`() {
        whenever(
            noticeRepository.findByIdAndStatusAndDeletedAtIsNull(
                noticeId,
                NoticeStatus.PUBLISHED,
            ),
        ).thenReturn(null)

        assertThatThrownBy {
            noticeQueryService.findById(noticeId)
        }.isInstanceOf(NoticeNotFoundException::class.java)

        verify(noticeRepository)
            .findByIdAndStatusAndDeletedAtIsNull(noticeId, NoticeStatus.PUBLISHED)
    }

    @Test
    fun `관리자용 공지 전체 조회 성공`() {
        val notices = listOf(createNotice())

        whenever(
            noticeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(),
        ).thenReturn(notices)

        val result = noticeQueryService.findAllForAdmin()

        verify(noticeRepository)
            .findAllByDeletedAtIsNullOrderByCreatedAtDesc()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `관리자용 공지 단건 조회 성공`() {
        val notice = createNotice()

        whenever(
            noticeRepository.findByIdAndDeletedAtIsNull(noticeId),
        ).thenReturn(notice)

        val result = noticeQueryService.findByIdForAdmin(noticeId)

        verify(noticeRepository)
            .findByIdAndDeletedAtIsNull(noticeId)

        assertThat(result).isEqualTo(notice)
    }

    @Test
    fun `관리자용 공지 단건 조회 실패 - 공지 없음`() {
        whenever(
            noticeRepository.findByIdAndDeletedAtIsNull(noticeId),
        ).thenReturn(null)

        assertThatThrownBy {
            noticeQueryService.findByIdForAdmin(noticeId)
        }.isInstanceOf(NoticeNotFoundException::class.java)

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
