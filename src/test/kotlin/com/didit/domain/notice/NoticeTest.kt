package com.didit.domain.notice

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class NoticeTest {
    private val adminId = UUID.randomUUID()

    private fun createNotice(): Notice {
        val request =
            NoticeRegisterRequest(
                title = "공지 제목",
                content = "공지 내용",
                status = NoticeStatus.PUBLISHED,
                sendPush = true,
            )
        return Notice.register(request, adminId)
    }

    @Test
    fun `register() 성공`() {
        val notice = createNotice()

        assertThat(notice.id).isNotNull()
        assertThat(notice.title).isEqualTo("공지 제목")
        assertThat(notice.content).isEqualTo("공지 내용")
        assertThat(notice.status).isEqualTo(NoticeStatus.PUBLISHED)
        assertThat(notice.sendPush).isTrue()
        assertThat(notice.adminId).isEqualTo(adminId)
        assertThat(notice.deletedAt).isNull()
    }

    @Test
    fun `register() 제목 비어있을시 실패`() {
        val request =
            NoticeRegisterRequest(
                title = "",
                content = "내용",
                status = NoticeStatus.DRAFT,
                sendPush = false,
            )

        assertThatThrownBy {
            Notice.register(request, adminId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("제목은 비어 있을 수 없습니다.")
    }

    @Test
    fun `register() 내용 비어있을시 실패`() {
        val request =
            NoticeRegisterRequest(
                title = "제목",
                content = "",
                status = NoticeStatus.DRAFT,
                sendPush = false,
            )

        assertThatThrownBy {
            Notice.register(request, adminId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("내용은 비어 있을 수 없습니다.")
    }

    @Test
    fun `update() 성공`() {
        val notice = createNotice()

        val request =
            NoticeRegisterRequest(
                title = "수정된 제목",
                content = "수정된 내용",
                status = NoticeStatus.PUBLISHED,
                sendPush = true,
            )

        notice.update(request)

        assertThat(notice.title).isEqualTo("수정된 제목")
        assertThat(notice.content).isEqualTo("수정된 내용")
        assertThat(notice.status).isEqualTo(NoticeStatus.PUBLISHED)
        assertThat(notice.sendPush).isTrue()
    }

    @Test
    fun `update() 제목 비어있을시 실패`() {
        val notice = createNotice()

        val request =
            NoticeRegisterRequest(
                title = "",
                content = "내용",
                status = NoticeStatus.DRAFT,
                sendPush = false,
            )

        assertThatThrownBy {
            notice.update(request)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("제목은 비어 있을 수 없습니다.")
    }

    @Test
    fun `update() 내용 비어있을시 실패`() {
        val notice = createNotice()

        val request =
            NoticeRegisterRequest(
                title = "제목",
                content = "",
                status = NoticeStatus.DRAFT,
                sendPush = false,
            )

        assertThatThrownBy {
            notice.update(request)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("내용은 비어 있을 수 없습니다.")
    }

    @Test
    fun `delete() 성공 - soft delete`() {
        val notice = createNotice()

        notice.delete()

        assertThat(notice.deletedAt).isNotNull()
        assertThat(notice.deletedAt).isBeforeOrEqualTo(LocalDateTime.now())
    }
}
