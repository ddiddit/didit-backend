package com.didit.adapter.webapi.notification

import com.didit.application.notification.provided.AdminNoticePushSender
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.notification.AdminNoticePushSendRequest
import com.didit.domain.notification.AdminNoticePushTargetType
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

class AdminNoticePushApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminNoticePushSender: AdminNoticePushSender = mock()

    override fun initController() = AdminNoticePushApi(adminNoticePushSender)

    @Test
    fun `send admin marketing push`() {
        val userId = UUID.randomUUID()
        val request =
            mapOf(
                "targetType" to "SELECTED_USERS",
                "userIds" to listOf(userId),
                "title" to "새로운 소식",
                "body" to "디딧의 새로운 기능을 확인해 보세요.",
                "link" to "/notices/1",
            )

        mockMvc
            .perform(
                post("/api/v1/admin/notice-pushes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "admin-notice-push/send",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("targetType").type(JsonFieldType.STRING).description("발송 대상 유형"),
                        fieldWithPath("userIds").type(JsonFieldType.ARRAY).description("선택 사용자 ID 목록"),
                        fieldWithPath("title").type(JsonFieldType.STRING).description("푸시 알림 제목"),
                        fieldWithPath("body").type(JsonFieldType.STRING).description("푸시 알림 본문"),
                        fieldWithPath("link").type(JsonFieldType.STRING).description("알림 클릭 시 이동할 링크"),
                    ),
                ),
            )

        val captor = argumentCaptor<AdminNoticePushSendRequest>()
        verify(adminNoticePushSender).send(captor.capture())

        assertThat(captor.firstValue.adminId).isEqualTo(adminId)
        assertThat(captor.firstValue.targetType).isEqualTo(AdminNoticePushTargetType.SELECTED_USERS)
        assertThat(captor.firstValue.userIds).containsExactly(userId)
        assertThat(captor.firstValue.title).isEqualTo("새로운 소식")
        assertThat(captor.firstValue.body).isEqualTo("디딧의 새로운 기능을 확인해 보세요.")
        assertThat(captor.firstValue.link).isEqualTo("/notices/1")
    }
}
