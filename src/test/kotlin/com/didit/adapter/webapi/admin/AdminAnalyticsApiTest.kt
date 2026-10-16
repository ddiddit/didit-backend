package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminAnalyticsFinder
import com.didit.application.admin.provided.AdminAnalyticsResult
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.CommonDocumentation
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class AdminAnalyticsApiTest : AdminAuthenticatedRestDocsSupport() {
    private val finder: AdminAnalyticsFinder = mock(AdminAnalyticsFinder::class.java)

    override fun initController() = AdminAnalyticsApi(finder)

    @Test
    fun `잘못된 분석 조회 기간은 400을 반환한다`() {
        val from = LocalDate.of(2026, 10, 31)
        val to = LocalDate.of(2026, 10, 30)
        whenever(finder.find(from, to)).thenThrow(BusinessException(ErrorCode.INVALID_REQUEST))

        mockMvc
            .perform(
                get("/api/v1/admin/analytics")
                    .param("from", from.toString())
                    .param("to", to.toString())
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isBadRequest)
            .andDo(
                document(
                    "admin/analytics-invalid-range",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(*CommonDocumentation.errorResponseFields()),
                ),
            )
    }

    @Test
    fun `공식 분석 지표 조회`() {
        val from = LocalDate.of(2026, 10, 1)
        val to = LocalDate.of(2026, 10, 30)
        whenever(finder.find(from, to)).thenReturn(
            AdminAnalyticsResult(
                from = from,
                to = to,
                retrospectiveCount = 10,
                completedCount = 8,
                completionRate = 80.0,
                retentionEligibleCount = 5,
                retentionReturnedCount = 2,
                retentionObservingCount = 1,
                secondCompletionRateWithin14Days = 40.0,
                returningUserCount = 20,
                newUserCount = 3,
                averageAnswerCount = 6.5,
                medianAnswerCount = 6.0,
                averageElapsedDurationSeconds = 420.0,
                medianElapsedDurationSeconds = 390.0,
            ),
        )

        mockMvc
            .perform(
                get("/api/v1/admin/analytics")
                    .param("from", from.toString())
                    .param("to", to.toString())
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "admin/analytics",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("from").description("조회 시작일 (포함, yyyy-MM-dd)"),
                        parameterWithName("to").description("조회 종료일 (포함, yyyy-MM-dd)"),
                    ),
                    responseFields(
                        fieldWithPath("data.from").type(JsonFieldType.STRING).description("조회 시작일"),
                        fieldWithPath("data.to").type(JsonFieldType.STRING).description("조회 종료일"),
                        fieldWithPath("data.retrospectiveCount").type(JsonFieldType.NUMBER).description("기간 내 시작한 회고 수"),
                        fieldWithPath("data.completedCount").type(JsonFieldType.NUMBER).description("기간 내 시작 후 현재 완료된 회고 수"),
                        fieldWithPath("data.completionRate").type(JsonFieldType.NUMBER).description("회고 완료율(%)"),
                        fieldWithPath("data.retentionEligibleCount").type(JsonFieldType.NUMBER).description("14일 관찰이 끝난 첫 회고 완료 사용자 수"),
                        fieldWithPath("data.retentionReturnedCount").type(JsonFieldType.NUMBER).description("14일 내 두 번째 회고를 완료한 사용자 수"),
                        fieldWithPath("data.retentionObservingCount").type(JsonFieldType.NUMBER).description("14일 관찰 중인 사용자 수"),
                        fieldWithPath(
                            "data.secondCompletionRateWithin14Days",
                        ).type(JsonFieldType.NUMBER).description("14일 내 두 번째 회고 완료율(%)"),
                        fieldWithPath("data.returningUserCount").type(JsonFieldType.NUMBER).description("출시 전 가입 후 출시 이후 복귀한 사용자 수"),
                        fieldWithPath("data.newUserCount").type(JsonFieldType.NUMBER).description("기간 내 신규 가입자 수"),
                        fieldWithPath("data.averageAnswerCount").type(JsonFieldType.NUMBER).optional().description("완료 회고당 평균 답변 수"),
                        fieldWithPath("data.medianAnswerCount").type(JsonFieldType.NUMBER).optional().description("완료 회고당 답변 수 중앙값"),
                        fieldWithPath(
                            "data.averageElapsedDurationSeconds",
                        ).type(JsonFieldType.NUMBER).optional().description("회고 생성부터 완료까지 평균 경과 초"),
                        fieldWithPath(
                            "data.medianElapsedDurationSeconds",
                        ).type(JsonFieldType.NUMBER).optional().description("회고 생성부터 완료까지 경과 초 중앙값"),
                    ),
                ),
            )
    }
}
