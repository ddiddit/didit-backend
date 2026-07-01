package com.didit.adapter.webapi.admin

import com.didit.application.admin.provided.AdminAchievementStatsFinder
import com.didit.application.admin.provided.AdminBadgeStat
import com.didit.application.admin.provided.AdminLevelStat
import com.didit.application.admin.provided.AdminMissionStat
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
import java.util.UUID

class AdminAchievementStatsApiTest : AdminAuthenticatedRestDocsSupport() {
    private val adminAchievementStatsFinder: AdminAchievementStatsFinder =
        mock(AdminAchievementStatsFinder::class.java)

    override fun initController() = AdminAchievementStatsApi(adminAchievementStatsFinder)

    @Test
    fun `레벨 분포 통계 조회`() {
        whenever(adminAchievementStatsFinder.getLevelStats()).thenReturn(
            listOf(AdminLevelStat(level = 1, userCount = 120)),
        )

        mockMvc
            .perform(get("/api/v1/admin/stats/levels").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/stats/levels",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].level").type(JsonFieldType.NUMBER).description("레벨"),
                        fieldWithPath("data[].userCount").type(JsonFieldType.NUMBER).description("해당 레벨 유저 수"),
                    ),
                ),
            )
    }

    @Test
    fun `미션 달성 통계 조회`() {
        whenever(adminAchievementStatsFinder.getMissionStats()).thenReturn(
            listOf(
                AdminMissionStat(
                    level = 2,
                    inProgress = 3,
                    completed = 5,
                    failed = 2,
                    total = 10,
                    completionRate = 50.0,
                ),
            ),
        )

        mockMvc
            .perform(get("/api/v1/admin/stats/missions").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/stats/missions",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].level").type(JsonFieldType.NUMBER).description("레벨"),
                        fieldWithPath("data[].inProgress").type(JsonFieldType.NUMBER).description("진행 중 수"),
                        fieldWithPath("data[].completed").type(JsonFieldType.NUMBER).description("완료 수"),
                        fieldWithPath("data[].failed").type(JsonFieldType.NUMBER).description("실패 수(미확인 포함)"),
                        fieldWithPath("data[].total").type(JsonFieldType.NUMBER).description("전체 수"),
                        fieldWithPath("data[].completionRate").type(JsonFieldType.NUMBER).description("완료율(%)"),
                    ),
                ),
            )
    }

    @Test
    fun `배지 획득 통계 조회`() {
        whenever(adminAchievementStatsFinder.getBadgeStats()).thenReturn(
            listOf(
                AdminBadgeStat(
                    badgeId = UUID.randomUUID(),
                    name = "첫 기록",
                    category = "CONSISTENCY",
                    conditionType = "CUMULATIVE_RETRO",
                    active = true,
                    acquiredCount = 150,
                ),
            ),
        )

        mockMvc
            .perform(get("/api/v1/admin/stats/badges").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "admin/stats/badges",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].badgeId").type(JsonFieldType.STRING).description("배지 ID"),
                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("배지명"),
                        fieldWithPath("data[].category").type(JsonFieldType.STRING).description("카테고리"),
                        fieldWithPath("data[].conditionType").type(JsonFieldType.STRING).description("획득 조건 유형"),
                        fieldWithPath("data[].active").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                        fieldWithPath("data[].acquiredCount").type(JsonFieldType.NUMBER).description("획득 유저 수"),
                    ),
                ),
            )
    }
}
