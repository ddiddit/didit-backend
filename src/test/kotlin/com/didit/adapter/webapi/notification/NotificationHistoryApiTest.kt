package com.didit.adapter.webapi.notification

import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.RestDocsSupport
import com.didit.domain.notification.NotificationHistory
import com.didit.support.NotificationHistoryFixture
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class NotificationHistoryApiTest : RestDocsSupport() {
    private val notificationHistoryFinder: NotificationHistoryFinder = mock(NotificationHistoryFinder::class.java)
    private val notificationHistoryRegister: NotificationHistoryRegister = mock(NotificationHistoryRegister::class.java)

    override fun initController() = NotificationHistoryApi(notificationHistoryFinder, notificationHistoryRegister)

    @Test
    fun `알림 히스토리 조회`() {
        val userId = UUID.randomUUID()
        val histories =
            listOf(
                NotificationHistory.create(NotificationHistoryFixture.createRequest(userId)),
            )
        whenever(notificationHistoryFinder.findAllByUserId(userId)).thenReturn(histories)

        mockMvc
            .perform(
                get("/api/v1/notification-histories")
                    .header("X-User-Id", userId.toString()),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "notification-history/find-all",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("알림 ID"),
                        fieldWithPath("data[].type").type(JsonFieldType.STRING).description("알림 타입"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("알림 제목"),
                        fieldWithPath("data[].body").type(JsonFieldType.STRING).description("알림 내용"),
                        fieldWithPath("data[].isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                        fieldWithPath("data[].createdAt").type(JsonFieldType.NULL).description("생성 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `알림 개별 읽음 처리`() {
        val userId = UUID.randomUUID()
        val id = UUID.randomUUID()

        mockMvc
            .perform(
                put("/api/v1/notification-histories/{id}/read", id)
                    .header("X-User-Id", userId.toString()),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-history/read",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    pathParameters(
                        parameterWithName("id").description("알림 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `알림 전체 읽음 처리`() {
        mockMvc
            .perform(
                put("/api/v1/notification-histories/read")
                    .header("X-User-Id", UUID.randomUUID().toString()),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-history/read-all",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                ),
            )
    }
}
