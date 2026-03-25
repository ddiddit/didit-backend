package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.notification.dto.UpdateConsentRequest
import com.didit.adapter.webapi.notification.dto.UpdateNotificationSettingRequest
import com.didit.application.auth.provided.UserFinder
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.notification.NotificationSetting
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalTime

class NotificationSettingApiTest : AuthenticatedRestDocsSupport() {
    private val notificationSettingFinder: NotificationSettingFinder = mock(NotificationSettingFinder::class.java)
    private val notificationSettingModifier: NotificationSettingModifier = mock(NotificationSettingModifier::class.java)
    private val userFinder: UserFinder = mock(UserFinder::class.java)

    override fun initController() = NotificationSettingApi(notificationSettingFinder, notificationSettingModifier, userFinder)

    @Test
    fun `알림 설정 조회`() {
        val setting = NotificationSetting.create(userId)
        val user =
            User(
                id = userId,
                provider = Provider.KAKAO,
                providerId = "kakao-provider-id",
            ).also { it.createConsent(marketingAgreed = false) }

        whenever(notificationSettingFinder.findByUserId(userId)).thenReturn(setting)
        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)

        mockMvc
            .perform(get("/api/v1/notification-settings"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "notification-setting/find",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.marketingAgreed").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의"),
                        fieldWithPath("data.nightPushConsent").type(JsonFieldType.BOOLEAN).description("야간 푸시 동의"),
                        fieldWithPath("data.enabled").type(JsonFieldType.BOOLEAN).description("회고 작성 알림 ON/OFF"),
                        fieldWithPath("data.reminderTime").type(JsonFieldType.STRING).description("알림 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `알림 설정 수정`() {
        val request = UpdateNotificationSettingRequest(enabled = true, reminderTime = LocalTime.of(21, 0))

        mockMvc
            .perform(
                put("/api/v1/notification-settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-setting/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("enabled").type(JsonFieldType.BOOLEAN).description("회고 작성 알림 ON/OFF"),
                        fieldWithPath("reminderTime").type(JsonFieldType.STRING).description("알림 시간"),
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "notification-setting/night-push-consent",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("consent").type(JsonFieldType.BOOLEAN).description("야간 푸시 동의 여부"),
                    ),
                ),
            )
    }
}
