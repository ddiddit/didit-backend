package com.didit.adapter.webapi.achievement

import com.didit.application.achievement.dto.BadgeResponse
import com.didit.application.achievement.provided.BadgeFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
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
        conditionType: BadgeConditionType,
        acquired: Boolean = false,
        acquiredAt: LocalDateTime? = null,
    ) = BadgeResponse(
        id = UUID.randomUUID(),
        name = conditionType.name,
        description = "설명",
        conditionType = conditionType.name,
        acquired = acquired,
        acquiredAt = acquiredAt,
    )

    @Test
    fun `배지 목록 조회`() {
        val badges =
            listOf(
                badgeResponse(BadgeConditionType.FIRST_RETRO, acquired = true, acquiredAt = LocalDateTime.now()),
                badgeResponse(BadgeConditionType.STREAK_3_DAYS, acquired = false),
                badgeResponse(BadgeConditionType.TOTAL_30, acquired = false),
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
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("배지 ID"),
                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("배지 이름"),
                        fieldWithPath("data[].description").type(JsonFieldType.STRING).description("배지 설명"),
                        fieldWithPath("data[].conditionType").type(JsonFieldType.STRING).description("배지 조건 타입"),
                        fieldWithPath("data[].acquired").type(JsonFieldType.BOOLEAN).description("획득 여부"),
                        fieldWithPath("data[].acquiredAt").type(JsonFieldType.STRING).description("획득 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `미알림 배지 조회`() {
        val badges =
            listOf(
                badgeResponse(BadgeConditionType.FIRST_RETRO, acquired = true, acquiredAt = LocalDateTime.now()),
                badgeResponse(BadgeConditionType.STREAK_3_DAYS, acquired = true, acquiredAt = LocalDateTime.now()),
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
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("배지 ID"),
                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("배지 이름"),
                        fieldWithPath("data[].description").type(JsonFieldType.STRING).description("배지 설명"),
                        fieldWithPath("data[].conditionType").type(JsonFieldType.STRING).description("배지 조건 타입"),
                        fieldWithPath("data[].acquired").type(JsonFieldType.BOOLEAN).description("획득 여부"),
                        fieldWithPath("data[].acquiredAt").type(JsonFieldType.STRING).description("획득 시간").optional(),
                    ),
                ),
            )
    }
}
