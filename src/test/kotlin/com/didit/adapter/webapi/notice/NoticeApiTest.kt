package com.didit.adapter.webapi.notice

import com.didit.application.notice.provided.NoticeFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeStatus
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class NoticeApiTest : AuthenticatedRestDocsSupport() {
    private val noticeFinder: NoticeFinder = mock(NoticeFinder::class.java)

    override fun initController() = NoticeApi(noticeFinder)

    @Test
    fun `공지사항 목록 조회`() {
        val notices =
            listOf(
                Notice(
                    id = UUID.randomUUID(),
                    title = "공지사항 제목",
                    content = "공지사항 내용",
                    status = NoticeStatus.PUBLISHED,
                    adminId = UUID.randomUUID(),
                    sendPush = false,
                    deletedAt = null,
                ),
            )

        whenever(noticeFinder.findAll()).thenReturn(notices)

        mockMvc
            .perform(get("/api/v1/notices"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "notice/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("공지사항 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("공지사항 제목"),
                    ),
                ),
            )
    }

    @Test
    fun `공지사항 상세 조회`() {
        val noticeId = UUID.randomUUID()

        val notice =
            Notice(
                id = noticeId,
                title = "공지사항 제목",
                content = "공지사항 내용",
                status = NoticeStatus.PUBLISHED,
                adminId = UUID.randomUUID(),
                sendPush = false,
                deletedAt = null,
            )

        val now = LocalDateTime.now()
        ReflectionTestUtils.setField(notice, "createdAt", now)
        ReflectionTestUtils.setField(notice, "updatedAt", now)

        whenever(noticeFinder.findById(notice.id)).thenReturn(notice)

        mockMvc
            .perform(get("/api/v1/notices/{noticeId}", notice.id))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "notice/detail",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("noticeId").description("공지사항 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("공지사항 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("공지사항 내용"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                    ),
                ),
            )
    }
}
