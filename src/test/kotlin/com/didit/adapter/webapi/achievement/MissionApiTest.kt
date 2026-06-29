package com.didit.adapter.webapi.achievement

import com.didit.application.achievement.dto.CurrentMissionResponse
import com.didit.application.achievement.dto.MissionInfo
import com.didit.application.achievement.dto.PopupStatus
import com.didit.application.achievement.dto.WeeklyStatus
import com.didit.application.achievement.provided.MissionFinder
import com.didit.application.achievement.provided.UserMissionRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class MissionApiTest : AuthenticatedRestDocsSupport() {
    private val missionFinder: MissionFinder = mock(MissionFinder::class.java)
    private val userMissionRegister: UserMissionRegister = mock(UserMissionRegister::class.java)

    override fun initController() = MissionApi(missionFinder, userMissionRegister)

    @Test
    fun `현재 미션을 조회한다`() {
        val currentMissionResponse =
            CurrentMissionResponse(
                currentLevel = 3,
                mission =
                    MissionInfo(
                        type = "CONSECUTIVE_WEEK",
                        title = "2주 연속 회고 작성하기",
                        description = "매주 한 번씩 회고를 작성하면 달성할 수 있어요",
                        progress = 1,
                        target = 2,
                        remainingDays = null,
                        cta = "회고 남기기",
                    ),
                weeklyStatus =
                    WeeklyStatus(
                        show = true,
                        weekStart = LocalDate.now(),
                        days = listOf(true, false, false, false, false, false, false),
                    ),
                popup = PopupStatus(exists = true, type = "LEVEL_UP"),
            )

        whenever(missionFinder.getCurrentMission(userId)).thenReturn(currentMissionResponse)

        mockMvc
            .perform(get("/api/v1/missions/current"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "mission/current",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.currentLevel").type(JsonFieldType.NUMBER).description("현재 레벨"),
                        fieldWithPath("data.mission.type").type(JsonFieldType.STRING).description("미션 타입"),
                        fieldWithPath("data.mission.title").type(JsonFieldType.STRING).description("미션 제목"),
                        fieldWithPath("data.mission.description").type(JsonFieldType.STRING).description("미션 설명"),
                        fieldWithPath("data.mission.progress").type(JsonFieldType.NUMBER).description("현재 진행도"),
                        fieldWithPath("data.mission.target").type(JsonFieldType.NUMBER).description("목표값"),
                        fieldWithPath("data.mission.remainingDays")
                            .type(JsonFieldType.NUMBER)
                            .description("남은 일수")
                            .optional(),
                        fieldWithPath("data.mission.cta").type(JsonFieldType.STRING).description("CTA 텍스트"),
                        fieldWithPath("data.weeklyStatus")
                            .type(JsonFieldType.OBJECT)
                            .description("주간 회고 현황")
                            .optional(),
                        fieldWithPath("data.weeklyStatus.show")
                            .type(JsonFieldType.BOOLEAN)
                            .description("주간 상황 노출 여부")
                            .optional(),
                        fieldWithPath("data.weeklyStatus.weekStart")
                            .type(JsonFieldType.STRING)
                            .description("주간 시작일")
                            .optional(),
                        fieldWithPath("data.weeklyStatus.days")
                            .type(JsonFieldType.ARRAY)
                            .description("요일별 완료 현황")
                            .optional(),
                        fieldWithPath("data.popup.exists").type(JsonFieldType.BOOLEAN).description("팝업 노출 여부"),
                        fieldWithPath("data.popup.type").type(JsonFieldType.STRING).description("팝업 타입").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `팝업 확인을 처리한다`() {
        mockMvc
            .perform(
                post("/api/v1/missions/level-up/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"type":"LEVEL_UP"}"""),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "mission/confirm-popup",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("type").type(JsonFieldType.STRING).description("팝업 타입 (LEVEL_UP, FAILURE)"),
                    ),
                ),
            )

        verify(userMissionRegister).confirmPopup(userId, "LEVEL_UP")
    }
}
