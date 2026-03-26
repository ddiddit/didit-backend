package com.didit.adapter.webapi.app

import com.didit.application.app.provided.AppConfigManager
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.app.AppConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
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

class AppConfigApiTest : AuthenticatedRestDocsSupport() {
    private val appConfigManager: AppConfigManager = mock(AppConfigManager::class.java)

    override fun initController() = AppConfigApi(appConfigManager)

    @Test
    fun `앱 설정 조회`() {
        val config =
            AppConfig(
                maintenanceMode = false,
                maintenanceMessage = null,
                minimumVersion = "0.0.0",
            )
        whenever(appConfigManager.getAppConfig()).thenReturn(config)

        mockMvc
            .perform(get("/api/v1/app/config"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "app-config/get",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.maintenanceMode").type(JsonFieldType.BOOLEAN).description("점검 모드 여부"),
                        fieldWithPath("data.maintenanceMessage")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("점검 메시지"),
                        fieldWithPath("data.minimumVersion").type(JsonFieldType.STRING).description("최소 지원 버전"),
                    ),
                ),
            )
    }

    @Test
    fun `앱 설정 수정`() {
        val request =
            mapOf(
                "maintenanceMode" to true,
                "maintenanceMessage" to "점검 중입니다.",
                "minimumVersion" to "1.0.0",
            )
        val updated =
            AppConfig(
                maintenanceMode = true,
                maintenanceMessage = "점검 중입니다.",
                minimumVersion = "1.0.0",
            )
        whenever(appConfigManager.updateAppConfig(any())).thenReturn(updated)

        mockMvc
            .perform(
                put("/api/v1/admin/settings/app-config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "app-config/update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("maintenanceMode").type(JsonFieldType.BOOLEAN).description("점검 모드 여부"),
                        fieldWithPath("maintenanceMessage").type(JsonFieldType.STRING).optional().description("점검 메시지"),
                        fieldWithPath("minimumVersion").type(JsonFieldType.STRING).description("최소 지원 버전"),
                    ),
                ),
            )
    }
}
