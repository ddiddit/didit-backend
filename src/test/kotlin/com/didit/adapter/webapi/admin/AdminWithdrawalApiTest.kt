package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminWithdrawalStatsFinder
import com.didit.application.admin.provided.AdminWithdrawalStatsResult
import com.didit.application.admin.provided.WithdrawalReasonCount
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminWithdrawalApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminWithdrawalStatsFinder: AdminWithdrawalStatsFinder = mock(AdminWithdrawalStatsFinder::class.java)

    override fun initController() = AdminWithdrawalApi(adminWithdrawalStatsFinder)

    @Test
    fun `탈퇴 통계 조회`() {
        val result = AdminWithdrawalStatsResult(
            total = 50,
            breakdown = listOf(
                WithdrawalReasonCount(reason = "NO_LONGER_NEEDED", count = 20, percentage = 40.0),
                WithdrawalReasonCount(reason = "MISSING_FEATURES", count = 15, percentage = 30.0),
                WithdrawalReasonCount(reason = "SERVICE_ISSUES", count = 10, percentage = 20.0),
                WithdrawalReasonCount(reason = "OTHER", count = 5, percentage = 10.0),
            ),
        )
        whenever(adminWithdrawalStatsFinder.getWithdrawalStats()).thenReturn(result)

        mockMvc
            .perform(get("/api/v1/admin/withdrawal-stats").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/withdrawal-stats",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.total").type(JsonFieldType.NUMBER).description("전체 탈퇴 수"),
                        fieldWithPath("data.breakdown[].reason")
                            .type(JsonFieldType.STRING)
                            .description("탈퇴 사유 (NO_LONGER_NEEDED, MISSING_FEATURES, SERVICE_ISSUES, DIFFICULT_TO_USE, SWITCHING_SERVICE, OTHER)"),
                        fieldWithPath("data.breakdown[].count").type(JsonFieldType.NUMBER).description("해당 사유 탈퇴 수"),
                        fieldWithPath("data.breakdown[].percentage").type(JsonFieldType.NUMBER).description("비율 (%)"),
                    ),
                ),
            )
    }
}
