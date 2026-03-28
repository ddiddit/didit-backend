package com.didit.adapter.webapi.notice

import com.didit.application.notice.provided.NoticeFinder
import com.didit.application.notice.provided.NoticeModifier
import com.didit.application.notice.provided.NoticeRegister
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class NoticeAdminApiTest : AdminAuthenticatedRestDocsSupport() {
    private val noticeFinder: NoticeFinder = mock(NoticeFinder::class.java)
    private val noticeRegister: NoticeRegister = mock(NoticeRegister::class.java)
    private val noticeModifier: NoticeModifier = mock(NoticeModifier::class.java)

    override fun initController() = NoticeAdminApi(noticeRegister, noticeFinder, noticeModifier)

    @Test
    fun `관리자 공지사항 목록 조회`() {
        val notices =
            listOf(
                Notice(
                    id = UUID.randomUUID(),
                    title = "공지사항 제목",
                    content = "공지사항 내용",
                    status = NoticeStatus.PUBLISHED,
                    adminId = adminId,
                    sendPush = false,
                    deletedAt = null,
                ).apply {
                    val now = LocalDateTime.now()
                    ReflectionTestUtils.setField(this, "createdAt", now)
                    ReflectionTestUtils.setField(this, "updatedAt", now)
                },
            )

        whenever(noticeFinder.findAllForAdmin()).thenReturn(notices)

        mockMvc
            .perform(get("/api/v1/admin/notices"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin-notice/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("공지사항 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("data[].status").type(JsonFieldType.STRING).description("공지 상태"),
                        fieldWithPath("data[].sendPush").type(JsonFieldType.BOOLEAN).description("푸시 여부"),
                        fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 공지사항 상세 조회`() {
        val noticeId = UUID.randomUUID()

        val notice =
            Notice(
                id = noticeId,
                title = "공지사항 제목",
                content = "공지사항 내용",
                status = NoticeStatus.PUBLISHED,
                adminId = adminId,
                sendPush = false,
                deletedAt = null,
            ).apply {
                val now = LocalDateTime.now()
                ReflectionTestUtils.setField(this, "createdAt", now)
                ReflectionTestUtils.setField(this, "updatedAt", now)
            }

        whenever(noticeFinder.findByIdForAdmin(noticeId)).thenReturn(notice)

        mockMvc
            .perform(get("/api/v1/admin/notices/{noticeId}", noticeId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin-notice/detail",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("noticeId").description("공지사항 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("공지사항 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("공지사항 내용"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("공지 상태"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시간"),
                        fieldWithPath("data.sendPush").type(JsonFieldType.BOOLEAN).description("푸시 발송 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 공지사항 생성`() {
        val notice =
            Notice(
                id = UUID.randomUUID(),
                title = "공지사항 제목",
                content = "공지사항 내용",
                status = NoticeStatus.PUBLISHED,
                adminId = adminId,
                sendPush = true,
                deletedAt = null,
            ).apply {
                val now = LocalDateTime.now()
                ReflectionTestUtils.setField(this, "createdAt", now)
                ReflectionTestUtils.setField(this, "updatedAt", now)
            }

        whenever(noticeRegister.register(any(), any())).thenReturn(notice)

        val request =
            mapOf(
                "title" to "공지사항 제목",
                "content" to "공지사항 내용",
                "status" to "PUBLISHED",
                "sendPush" to true,
            )

        mockMvc
            .perform(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin-notice/register",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("content").type(JsonFieldType.STRING).description("공지사항 내용"),
                        fieldWithPath("status").type(JsonFieldType.STRING).description("공지 상태"),
                        fieldWithPath("sendPush").type(JsonFieldType.BOOLEAN).description("푸시 발송 여부"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("공지사항 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("공지사항 내용"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("공지 상태"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시간"),
                        fieldWithPath("data.sendPush").type(JsonFieldType.BOOLEAN).description("푸시 발송 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 공지사항 수정`() {
        val noticeId = UUID.randomUUID()

        val notice =
            Notice(
                id = noticeId,
                title = "수정된 제목",
                content = "수정된 내용",
                status = NoticeStatus.PUBLISHED,
                adminId = adminId,
                sendPush = true,
                deletedAt = null,
            ).apply {
                val now = LocalDateTime.now()
                ReflectionTestUtils.setField(this, "createdAt", now)
                ReflectionTestUtils.setField(this, "updatedAt", now)
            }

        whenever(noticeModifier.modify(any(), any(), any())).thenReturn(notice)

        val request =
            mapOf(
                "title" to "수정된 제목",
                "content" to "수정된 내용",
                "status" to "PUBLISHED",
                "sendPush" to true,
            )

        mockMvc
            .perform(
                put("/api/v1/admin/notices/{noticeId}", noticeId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin-notice/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("noticeId").description("공지사항 ID"),
                    ),
                    requestFields(
                        fieldWithPath("title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("content").type(JsonFieldType.STRING).description("공지사항 내용"),
                        fieldWithPath("status").type(JsonFieldType.STRING).description("공지 상태"),
                        fieldWithPath("sendPush").type(JsonFieldType.BOOLEAN).description("푸시 발송 여부"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("공지사항 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("공지사항 제목"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("공지사항 내용"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("공지 상태"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 시간"),
                        fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 시간"),
                        fieldWithPath("data.sendPush").type(JsonFieldType.BOOLEAN).description("푸시 발송 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 공지사항 삭제`() {
        val noticeId = UUID.randomUUID()

        mockMvc
            .perform(delete("/api/v1/admin/notices/{noticeId}", noticeId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin-notice/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    pathParameters(
                        parameterWithName("noticeId").description("공지사항 ID"),
                    ),
                ),
            )
    }
}
