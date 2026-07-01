package com.didit.adapter.webapi.achievement

import com.didit.application.achievement.dto.BadgeResponse
import com.didit.application.achievement.provided.BadgeFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.achievement.BadgeCategory
import com.didit.domain.achievement.BadgeConditionType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class BadgeApiTest : AuthenticatedRestDocsSupport() {
    private val badgeFinder: BadgeFinder = mock(BadgeFinder::class.java)

    override fun initController() = BadgeApi(badgeFinder)

    private fun badgeResponse(
        conditionType: BadgeConditionType = BadgeConditionType.CUMULATIVE_RETRO,
        category: BadgeCategory = BadgeCategory.CONSISTENCY,
        threshold: Int = 1,
        acquired: Boolean = false,
        acquiredAt: LocalDateTime? = null,
    ) = BadgeResponse(
        id = UUID.randomUUID(),
        name = "첫 기록",
        description = "첫 회고 저장 완료",
        category = category.name,
        conditionType = conditionType.name,
        threshold = threshold,
        iconUrl = null,
        congratsTitle = null,
        congratsMessage = null,
        acquired = acquired,
        acquiredAt = acquiredAt,
    )

    private fun responseFields() =
        responseFields(
            fieldWithPath("data[].id").type(JsonFieldType.STRING).description("배지 ID"),
            fieldWithPath("data[].name").type(JsonFieldType.STRING).description("배지 이름"),
            fieldWithPath("data[].description").type(JsonFieldType.STRING).description("배지 설명"),
            fieldWithPath("data[].category").type(JsonFieldType.STRING).description("배지 카테고리"),
            fieldWithPath("data[].conditionType").type(JsonFieldType.STRING).description("배지 조건 타입"),
            fieldWithPath("data[].threshold").type(JsonFieldType.NUMBER).description("조건 임계값"),
            fieldWithPath("data[].iconUrl").type(JsonFieldType.STRING).description("배지 아이콘 URL").optional(),
            fieldWithPath("data[].congratsTitle").type(JsonFieldType.STRING).description("축하 타이틀").optional(),
            fieldWithPath("data[].congratsMessage").type(JsonFieldType.STRING).description("축하 메시지").optional(),
            fieldWithPath("data[].acquired").type(JsonFieldType.BOOLEAN).description("획득 여부"),
            fieldWithPath("data[].acquiredAt").type(JsonFieldType.STRING).description("획득 시간").optional(),
        )

    @Test
    fun `배지 목록 조회`() {
        val badges =
            listOf(
                badgeResponse(BadgeConditionType.CUMULATIVE_RETRO, acquired = true, acquiredAt = LocalDateTime.now()),
                badgeResponse(BadgeConditionType.DAILY_ACCESS_STREAK, category = BadgeCategory.ACCESS, threshold = 7),
                badgeResponse(BadgeConditionType.PROJECT_COUNT, category = BadgeCategory.PROJECT, threshold = 3),
            )
        whenever(badgeFinder.findAll(userId)).thenReturn(badges)

        mockMvc
            .perform(get("/api/v1/badges"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "badge/find-all",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(),
                ),
            )
    }

    @Test
    fun `최근 획득 배지 조회`() {
        val badges =
            listOf(
                badgeResponse(
                    BadgeConditionType.DAILY_ACCESS_STREAK,
                    category = BadgeCategory.ACCESS,
                    threshold = 7,
                    acquired = true,
                    acquiredAt = LocalDateTime.now(),
                ),
            )
        whenever(badgeFinder.findRecent(userId)).thenReturn(badges)

        mockMvc
            .perform(get("/api/v1/badges/recent"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "badge/find-recent",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(),
                ),
            )
    }

    @Test
    fun `미알림 배지 조회`() {
        val badges =
            listOf(
                badgeResponse(BadgeConditionType.CUMULATIVE_RETRO, acquired = true, acquiredAt = LocalDateTime.now()),
                badgeResponse(
                    BadgeConditionType.DAILY_ACCESS_STREAK,
                    category = BadgeCategory.ACCESS,
                    threshold = 7,
                    acquired = true,
                    acquiredAt = LocalDateTime.now(),
                ),
            )
        whenever(badgeFinder.findUnnotified(userId)).thenReturn(badges)

        mockMvc
            .perform(get("/api/v1/badges/popup"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "badge/popup",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(),
                ),
            )
    }
}
