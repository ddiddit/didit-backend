package com.didit.adapter.webapi.home

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.support.UserFixture
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class HomeApiTest : AuthenticatedRestDocsSupport() {
    private val userFinder: UserFinder = mock(UserFinder::class.java)
    private val retrospectiveFinder: RetrospectiveFinder = mock(RetrospectiveFinder::class.java)

    override fun initController() = HomeApi(userFinder, retrospectiveFinder)

    @Test
    fun `홈 조회`() {
        val user = UserFixture.createOnboarded()
        val retro =
            Retrospective.create(userId).apply {
                addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
            }

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(retrospectiveFinder.findRecentByUserId(userId, 5)).thenReturn(listOf(retro))
        whenever(retrospectiveFinder.countByUserIdAndDate(userId, LocalDate.now())).thenReturn(1)
        whenever(retrospectiveFinder.findLatestCompletedByUserId(userId)).thenReturn(null)

        mockMvc
            .perform(get("/api/v1/home"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "home/get",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("data.todayRetrospectiveCount").type(JsonFieldType.NUMBER).description("오늘 회고 횟수"),
                        fieldWithPath("data.recentRetrospectives").type(JsonFieldType.ARRAY).description("최근 회고 목록"),
                        fieldWithPath("data.recentRetrospectives[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.recentRetrospectives[].title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data.recentRetrospectives[].feedback").type(JsonFieldType.STRING).description("AI 피드백").optional(),
                        fieldWithPath("data.recentRetrospectives[].createdAt").type(JsonFieldType.NULL).description("생성 시간"),
                        fieldWithPath("data.latestFeedback").type(JsonFieldType.STRING).description("최근 AI 피드백").optional(),
                    ),
                ),
            )
    }
}
