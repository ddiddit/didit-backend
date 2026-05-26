package com.didit.adapter.webapi.notification

import com.didit.application.notification.provided.AdminNoticeEmailSender
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.notification.AdminNoticeEmailSendRequest
import com.didit.domain.notification.AdminNoticeEmailTargetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class AdminNoticeEmailApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminNoticeEmailSender: AdminNoticeEmailSender = mock()

    override fun initController() = AdminNoticeEmailApi(adminNoticeEmailSender)

    @Test
    fun `관리자 공지 이메일 발송`() {
        val userId = UUID.randomUUID()
        val request =
            mapOf(
                "targetType" to "SELECTED_USERS",
                "userIds" to listOf(userId),
                "subject" to "공지 제목",
                "body" to "<p>공지 내용</p>",
            )

        mockMvc
            .perform(
                post("/api/v1/admin/notice-emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin-notice-email/send",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("targetType").type(JsonFieldType.STRING).description("발송 대상 타입"),
                        fieldWithPath("userIds").type(JsonFieldType.ARRAY).description("선택 사용자 ID 목록"),
                        fieldWithPath("subject").type(JsonFieldType.STRING).description("이메일 제목"),
                        fieldWithPath("body").type(JsonFieldType.STRING).description("이메일 본문 HTML"),
                    ),
                ),
            )

        val captor = argumentCaptor<AdminNoticeEmailSendRequest>()
        verify(adminNoticeEmailSender).send(captor.capture())

        assertThat(captor.firstValue.adminId).isEqualTo(adminId)
        assertThat(captor.firstValue.targetType).isEqualTo(AdminNoticeEmailTargetType.SELECTED_USERS)
        assertThat(captor.firstValue.userIds).containsExactly(userId)
        assertThat(captor.firstValue.subject).isEqualTo("공지 제목")
        assertThat(captor.firstValue.body).isEqualTo("<p>공지 내용</p>")
    }
}