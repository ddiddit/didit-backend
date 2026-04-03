package com.didit.application.notice

import com.didit.application.notice.exception.NoticeForbiddenException
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
class NoticeModifierServiceTest {
    @Mock
    lateinit var noticeRepository: NoticeRepository

    @InjectMocks
    lateinit var noticeModifierService: NoticeModifierService

    private val adminId = UUID.randomUUID()
    private val otherAdminId = UUID.randomUUID()
    private val noticeId = UUID.randomUUID()

    @Test
    fun `공지 수정 성공`() {
        val notice = createNotice(adminId)
        val request = validRequest()

        whenever(noticeRepository.findByIdAndDeletedAtIsNull(noticeId))
            .thenReturn(notice)

        noticeModifierService.modify(request, noticeId, adminId)

        verify(noticeRepository).findByIdAndDeletedAtIsNull(noticeId)

        assertThat(notice.title).isEqualTo(request.title)
        assertThat(notice.content).isEqualTo(request.content)
        assertThat(notice.status).isEqualTo(request.status)
        assertThat(notice.sendPush).isEqualTo(request.sendPush)
    }

    @Test
    fun `공지 수정 실패 - 공지 없음`() {
        whenever(noticeRepository.findByIdAndDeletedAtIsNull(noticeId))
            .thenReturn(null)

        assertThatThrownBy {
            noticeModifierService.modify(validRequest(), noticeId, adminId)
        }.isInstanceOf(NoticeNotFoundException::class.java)

        verify(noticeRepository).findByIdAndDeletedAtIsNull(noticeId)
    }

    @Test
    fun `공지 수정 실패 - 권한 없음`() {
        val notice = createNotice(adminId)

        whenever(noticeRepository.findByIdAndDeletedAtIsNull(noticeId))
            .thenReturn(notice)

        assertThatThrownBy {
            noticeModifierService.modify(validRequest(), noticeId, otherAdminId)
        }.isInstanceOf(NoticeForbiddenException::class.java)

        verify(noticeRepository).findByIdAndDeletedAtIsNull(noticeId)
    }

    @Test
    fun `공지 삭제 성공`() {
        val notice = createNotice(adminId)

        whenever(noticeRepository.findByIdAndDeletedAtIsNull(noticeId))
            .thenReturn(notice)

        noticeModifierService.delete(noticeId, adminId)

        verify(noticeRepository).findByIdAndDeletedAtIsNull(noticeId)
        assertThat(notice.deletedAt).isNotNull()
    }

    @Test
    fun `공지 삭제 실패 - 공지 없음`() {
        whenever(noticeRepository.findByIdAndDeletedAtIsNull(noticeId))
            .thenReturn(null)

        assertThatThrownBy {
            noticeModifierService.delete(noticeId, adminId)
        }.isInstanceOf(NoticeNotFoundException::class.java)

        verify(noticeRepository).findByIdAndDeletedAtIsNull(noticeId)
    }

    @Test
    fun `공지 삭제 실패 - 권한 없음`() {
        val notice = createNotice(adminId)

        whenever(noticeRepository.findByIdAndDeletedAtIsNull(noticeId))
            .thenReturn(notice)

        assertThatThrownBy {
            noticeModifierService.delete(noticeId, otherAdminId)
        }.isInstanceOf(NoticeForbiddenException::class.java)

        verify(noticeRepository).findByIdAndDeletedAtIsNull(noticeId)
    }

    private fun createNotice(adminId: UUID): Notice =
        Notice.register(
            NoticeRegisterRequest(
                title = "기존 제목",
                content = "기존 내용",
                status = NoticeStatus.DRAFT,
                sendPush = false,
            ),
            adminId,
        )

    private fun validRequest() =
        NoticeRegisterRequest(
            title = "제목",
            content = "내용",
            status = NoticeStatus.PUBLISHED,
            sendPush = true,
        )
}
