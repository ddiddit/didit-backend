package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.notification.dto.DeviceTokenRequest
import com.didit.application.notification.provided.DeviceTokenRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.RestDocsSupport
import com.didit.domain.notification.DeviceType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class DeviceTokenApiTest : RestDocsSupport() {
    private val deviceTokenRegister: DeviceTokenRegister = mock(DeviceTokenRegister::class.java)

    override fun initController() = DeviceTokenApi(deviceTokenRegister)

    @Test
    fun `기기 토큰 등록`() {
        val request =
            DeviceTokenRequest(
                token = "fcm-token-example",
                deviceType = DeviceType.IOS,
            )

        mockMvc
            .perform(
                post("/api/v1/device-tokens")
                    .header("X-User-Id", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andDo(
                document(
                    "device-token/register",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    requestFields(
                        fieldWithPath("token").type(JsonFieldType.STRING).description("FCM 토큰"),
                        fieldWithPath("deviceType").type(JsonFieldType.STRING).description("기기 타입 (IOS, ANDROID)"),
                    ),
                ),
            )
    }

    @Test
    fun `기기 토큰 삭제`() {
        mockMvc
            .perform(
                delete("/api/v1/device-tokens?deviceType={deviceType}", DeviceType.IOS.name)
                    .header("X-User-Id", UUID.randomUUID().toString()),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "device-token/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestHeaders(
                        headerWithName("X-User-Id").description("사용자 ID"),
                    ),
                    queryParameters(
                        parameterWithName("deviceType").description("기기 타입 (IOS, ANDROID)"),
                    ),
                ),
            )
    }
}
