package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminStatsFinder
import com.didit.application.admin.provided.AdminStatsResult
import com.didit.application.admin.provided.DailyRetroCount
import com.didit.application.admin.provided.RecentInquirySummary
import com.didit.application.admin.provided.RecentUserSummary
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AdminStatsApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminStatsFinder: AdminStatsFinder = mock(AdminStatsFinder::class.java)

    override fun initController() = AdminStatsApi(adminStatsFinder)

    @Test
    fun `대시보드 통계 조회`() {
        val result =
            AdminStatsResult(
                totalUsers = 1000,
                newUsersToday = 5,
                totalRetrospects = 500,
                unansweredInquiries = 3,
                dau = 120,
                todayRetrospects = 20,
                weeklyRetroTrend =
                    listOf(
                        DailyRetroCount(date = LocalDate.of(2026, 6, 10), count = 30),
                        DailyRetroCount(date = LocalDate.of(2026, 6, 11), count = 25),
                    ),
                todayCompletionRate = 62.5,
                totalInputTokens = 1_200_000,
                totalOutputTokens = 800_000,
                todayInputTokens = 30_000,
                todayOutputTokens = 12_000,
                textAnswerCount = 1500,
                voiceAnswerCount = 600,
                recentUsers =
                    listOf(
                        RecentUserSummary(
                            id = UUID.randomUUID(),
                            email = "user@example.com",
                            nickname = "디딧유저",
                            job = "DEVELOPER",
                            createdAt = LocalDateTime.of(2026, 6, 1, 0, 0),
                        ),
                    ),
                recentInquiries =
                    listOf(
                        RecentInquirySummary(
                            id = UUID.randomUUID(),
                            type = "BUG",
                            content = "오류가 발생합니다.",
                            status = "PENDING",
                            createdAt = LocalDateTime.of(2026, 6, 1, 0, 0),
                        ),
                    ),
            )
        whenever(adminStatsFinder.getStats()).thenReturn(result)

        mockMvc
            .perform(get("/api/v1/admin/stats").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/stats",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.totalUsers").type(JsonFieldType.NUMBER).description("전체 유저 수"),
                        fieldWithPath("data.newUsersToday").type(JsonFieldType.NUMBER).description("오늘 신규 가입자 수"),
                        fieldWithPath("data.totalRetrospects").type(JsonFieldType.NUMBER).description("전체 완료 회고 수"),
                        fieldWithPath("data.unansweredInquiries").type(JsonFieldType.NUMBER).description("미답변 문의 수"),
                        fieldWithPath("data.dau").type(JsonFieldType.NUMBER).description("오늘 일간 활성 유저 수"),
                        fieldWithPath("data.todayRetrospects").type(JsonFieldType.NUMBER).description("오늘 완료 회고 수"),
                        fieldWithPath("data.weeklyRetroTrend[].date").type(JsonFieldType.STRING).description("날짜"),
                        fieldWithPath("data.weeklyRetroTrend[].count").type(JsonFieldType.NUMBER).description("완료 회고 수"),
                        fieldWithPath("data.todayCompletionRate").type(JsonFieldType.NUMBER).description("오늘 완료율(%) — 오늘 생성된 회고 중 완료 비율"),
                        fieldWithPath("data.totalInputTokens").type(JsonFieldType.NUMBER).description("전체 입력 토큰 사용량"),
                        fieldWithPath("data.totalOutputTokens").type(JsonFieldType.NUMBER).description("전체 출력 토큰 사용량"),
                        fieldWithPath("data.todayInputTokens").type(JsonFieldType.NUMBER).description("오늘(완료일 기준) 입력 토큰 사용량"),
                        fieldWithPath("data.todayOutputTokens").type(JsonFieldType.NUMBER).description("오늘(완료일 기준) 출력 토큰 사용량"),
                        fieldWithPath("data.textAnswerCount").type(JsonFieldType.NUMBER).description("텍스트 답변 수"),
                        fieldWithPath("data.voiceAnswerCount").type(JsonFieldType.NUMBER).description("음성(STT) 답변 수"),
                        fieldWithPath("data.recentUsers[].id").type(JsonFieldType.STRING).description("유저 ID"),
                        fieldWithPath("data.recentUsers[].email").type(JsonFieldType.STRING).optional().description("이메일"),
                        fieldWithPath("data.recentUsers[].nickname").type(JsonFieldType.STRING).optional().description("닉네임"),
                        fieldWithPath("data.recentUsers[].job").type(JsonFieldType.STRING).optional().description("직무"),
                        fieldWithPath("data.recentUsers[].createdAt").type(JsonFieldType.STRING).optional().description("가입 일시"),
                        fieldWithPath("data.recentInquiries[].id").type(JsonFieldType.STRING).description("문의 ID"),
                        fieldWithPath("data.recentInquiries[].type").type(JsonFieldType.STRING).description("문의 유형"),
                        fieldWithPath("data.recentInquiries[].content").type(JsonFieldType.STRING).description("문의 내용"),
                        fieldWithPath("data.recentInquiries[].status").type(JsonFieldType.STRING).description("답변 상태 (PENDING, ANSWERED)"),
                        fieldWithPath("data.recentInquiries[].createdAt").type(JsonFieldType.STRING).optional().description("문의 일시"),
                    ),
                ),
            )
    }
}
