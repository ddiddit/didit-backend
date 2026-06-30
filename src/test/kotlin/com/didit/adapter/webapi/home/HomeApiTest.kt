package com.didit.adapter.webapi.home

import com.didit.application.auth.provided.UserFinder
import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.organization.Project
import com.didit.domain.organization.Tag
import com.didit.domain.shared.ServiceTime
import com.didit.support.RetrospectiveFixture
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
import java.util.UUID

class HomeApiTest : AuthenticatedRestDocsSupport() {
    private val userFinder: UserFinder = mock(UserFinder::class.java)
    private val retrospectiveFinder: RetrospectiveFinder = mock(RetrospectiveFinder::class.java)
    private val notificationHistoryFinder: NotificationHistoryFinder = mock(NotificationHistoryFinder::class.java)

    override fun initController() = HomeApi(userFinder, retrospectiveFinder, notificationHistoryFinder)

    @Test
    fun `홈 조회`() {
        val user = UserFixture.createOnboarded()
        val retro = RetrospectiveFixture.createCompleted(userId)

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(retrospectiveFinder.findRecentByUserId(userId, 5)).thenReturn(listOf(retro))
        whenever(retrospectiveFinder.countByUserIdAndDate(userId, ServiceTime.today())).thenReturn(1)

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
                        fieldWithPath("data.recentRetrospectives[].summary").type(JsonFieldType.STRING).description("회고 요약").optional(),
                        fieldWithPath("data.recentRetrospectives[].doneWork").type(JsonFieldType.STRING).description("완료한 작업").optional(),
                        fieldWithPath("data.recentRetrospectives[].completedAt").type(JsonFieldType.STRING).description("완료 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `홈 조회 v2`() {
        val user = UserFixture.createOnboarded()
        val result =
            RetrospectiveDetailResult(
                retrospective = RetrospectiveFixture.createCompleted(userId),
                project = Project(UUID.randomUUID(), userId, "프로젝트 이름"),
                tags = listOf(Tag(UUID.randomUUID(), userId, "태그1"), Tag(UUID.randomUUID(), userId, "태그2")),
            )

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)
        whenever(retrospectiveFinder.findRecentWithProjectAndTagsByUserId(userId, 5)).thenReturn(listOf(result))
        whenever(retrospectiveFinder.countByUserIdAndDate(userId, ServiceTime.today())).thenReturn(1)
        whenever(notificationHistoryFinder.hasUnread(userId)).thenReturn(true)

        mockMvc
            .perform(get("/api/v2/home"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "home/v2/get",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("data.todayRetrospectiveCount").type(JsonFieldType.NUMBER).description("오늘 회고 횟수"),
                        fieldWithPath("data.hasUnreadNotification").type(JsonFieldType.BOOLEAN).description("안 읽은 알림 존재 여부"),
                        fieldWithPath("data.recentRetrospectives").type(JsonFieldType.ARRAY).description("최근 회고 목록"),
                        fieldWithPath("data.recentRetrospectives[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.recentRetrospectives[].title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data.recentRetrospectives[].summary").type(JsonFieldType.STRING).description("회고 요약").optional(),
                        fieldWithPath("data.recentRetrospectives[].completedAt")
                            .type(JsonFieldType.STRING)
                            .description("완료 시간")
                            .optional(),
                        fieldWithPath("data.recentRetrospectives[].projectName")
                            .type(JsonFieldType.STRING)
                            .description("프로젝트 이름")
                            .optional(),
                        fieldWithPath("data.recentRetrospectives[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                        fieldWithPath("data.recentRetrospectives[].tags[].id").type(JsonFieldType.STRING).description("태그 ID"),
                        fieldWithPath("data.recentRetrospectives[].tags[].name").type(JsonFieldType.STRING).description("태그 이름"),
                    ),
                ),
            )
    }
}
