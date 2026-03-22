package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.notification.dto.UpdateConsentRequest
import com.didit.adapter.webapi.notification.dto.UpdateNotificationSettingRequest
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.RestDocsSupport
import com.didit.domain.notification.NotificationSetting
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalTime
import java.util.UUID

class NotificationSettingApiTest : RestDocsSupport() {
    private val notificationSettingFinder: NotificationSettingFinder = mock(NotificationSettingFinder::class.java)
    private val notificationSettingModifier: NotificationSettingModifier = mock(NotificationSettingModifier::class.java)

    override fun initController() = NotificationSettingApi(notificationSettingFinder, notificationSettingModifier)

    @Test
    fun `알림 설정 조회`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        whenever(notificationSettingFinder.findByUserId(userId)).thenReturn(setting)

        mockMvc
            .perform(
                get("/api/v1/notification-settings")
                    .header("X-User-Id", userId.toString()),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "notification-setting/find",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.enabled").type(JsonFieldType.BOOLEAN).description("회고 작성 알림 ON/OFF"),
                        fieldWithPath("data.reminderTime").type(JsonFieldType.STRING).description("알림 시간"),
                        fieldWithPath("data.marketingConsent").type(JsonFieldType.BOOLEAN).description("마케팅 수신 동의"),
                        fieldWithPath("data.nightPushConsent").type(JsonFieldType.BOOLEAN).description("야간 푸시 동의"),
                    ),
                ),
            )
    }

    @Test
    fun `알림 설정 수정`() {
        val request =
            UpdateNotificationSettingRequest(
                enabled = true,
                reminderTime = LocalTime.of(21, 0),
            )

        mockMvc
            .perform(
                put("/api/v1/notification-settings")
                    .header("X-User-Id", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-setting/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    requestFields(
                        fieldWithPath("enabled").type(JsonFieldType.BOOLEAN).description("회고 작성 알림 ON/OFF"),
                        fieldWithPath("reminderTime").type(JsonFieldType.STRING).description("알림 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `마케팅 동의 수정`() {
        val request = UpdateConsentRequest(consent = true)

        mockMvc
            .perform(
                put("/api/v1/notification-settings/marketing-consent")
                    .header("X-User-Id", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-setting/marketing-consent",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    requestFields(
                        fieldWithPath("consent").type(JsonFieldType.BOOLEAN).description("마케팅 수신 동의 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `야간 푸시 동의 수정`() {
        val request = UpdateConsentRequest(consent = true)

        mockMvc
            .perform(
                put("/api/v1/notification-settings/night-push-consent")
                    .header("X-User-Id", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-setting/night-push-consent",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    requestFields(
                        fieldWithPath("consent").type(JsonFieldType.BOOLEAN).description("야간 푸시 동의 여부"),
                    ),
                ),
            )
    }
}
