package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.retrospect.dto.SaveRetrospectiveRequest
import com.didit.adapter.webapi.retrospect.dto.SubmitAnswerRequest
import com.didit.adapter.webapi.retrospect.dto.UpdateTitleRequest
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.DeepQuestionResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.provided.RetrospectiveRegister
import com.didit.application.retrospect.provided.SearchHistoryFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.retrospect.SearchHistory
import com.didit.support.RetrospectiveFixture
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RetrospectApiTest : AuthenticatedRestDocsSupport() {
    private val retrospectiveRegister: RetrospectiveRegister = mock(RetrospectiveRegister::class.java)
    private val retrospectiveFinder: RetrospectiveFinder = mock(RetrospectiveFinder::class.java)
    private val searchHistoryFinder: SearchHistoryFinder = mock(SearchHistoryFinder::class.java)

    override fun initController() =
        RetrospectApi(
            retrospectiveRegister,
            retrospectiveFinder,
            searchHistoryFinder,
        )

    private val retrospectiveId = UUID.randomUUID()

    private fun retrospectiveWithQ1(): Retrospective {
        val retro = Retrospective.create(userId)
        retro.addMessage(ChatMessage.question(retro, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
        return retro
    }

    private fun completedRetrospective() = RetrospectiveFixture.createCompleted(userId)

    private fun aiSummaryResponse() =
        AISummaryResponse(
            title = "오늘의 회고",
            feedback = "오늘 작업을 잘 마무리하셨네요.",
            insight = "문제를 작게 나누는 것이 중요합니다.",
            doneWork = "로그인 API 연동 작업을 완료했습니다.",
            blockedPoint = "토큰 만료 처리 로직이 복잡했습니다.",
            solutionProcess = "공식 문서를 참고하여 해결했습니다.",
            lessonLearned = "초반에 에러 처리를 설계해두면 편합니다.",
        )

    @Test
    fun `회고 시작`() {
        val retro = retrospectiveWithQ1()
        whenever(retrospectiveRegister.start(userId)).thenReturn(retro)

        mockMvc
            .perform(post("/api/v1/retrospectives"))
            .andExpect(status().isCreated)
            .andDo(
                document(
                    "retrospect/start",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.firstQuestionType").type(JsonFieldType.STRING).description("첫 번째 질문 타입"),
                        fieldWithPath("data.firstQuestionContent").type(JsonFieldType.STRING).description("첫 번째 질문 내용"),
                    ),
                ),
            )
    }

    @Test
    fun `답변 제출`() {
        val request = SubmitAnswerRequest(content = "로그인 API 연동 작업을 했습니다.")
        val response =
            SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q2,
                nextQuestionContent = "진행하면서 어떤 시도, 혹은 어려움이 있었나요?",
                isReadyToComplete = false,
            )
        whenever(retrospectiveRegister.submitAnswer(retrospectiveId, userId, request.content))
            .thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/retrospectives/{retrospectiveId}/answers", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/submit-answer",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    requestFields(
                        fieldWithPath("content").type(JsonFieldType.STRING).description("답변 내용"),
                        fieldWithPath("inputType").type(JsonFieldType.STRING).description("입력 타입 (TEXT, STT)"),
                    ),
                    responseFields(
                        fieldWithPath("data.nextQuestionType").type(JsonFieldType.STRING).description("다음 질문 타입").optional(),
                        fieldWithPath("data.nextQuestionContent").type(JsonFieldType.STRING).description("다음 질문 내용").optional(),
                        fieldWithPath("data.isReadyToComplete").type(JsonFieldType.BOOLEAN).description("완료 가능 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `음성 답변 제출`() {
        val response =
            SubmitAnswerResponse(
                content = "로그인 API 연동 작업을 했습니다.",
                nextQuestionType = QuestionType.Q2,
                nextQuestionContent = "진행하면서 어떤 시도, 혹은 어려움이 있었나요?",
                isReadyToComplete = false,
            )
        whenever(retrospectiveRegister.submitVoiceAnswer(any(), any(), any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
                    .multipart("/api/v1/retrospectives/{retrospectiveId}/answers/voice", retrospectiveId)
                    .file("file", ByteArray(100))
                    .contentType(MediaType.MULTIPART_FORM_DATA),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/submit-voice-answer",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("STT 변환된 텍스트").optional(),
                        fieldWithPath("data.nextQuestionType").type(JsonFieldType.STRING).description("다음 질문 타입").optional(),
                        fieldWithPath("data.nextQuestionContent").type(JsonFieldType.STRING).description("다음 질문 내용").optional(),
                        fieldWithPath("data.isReadyToComplete").type(JsonFieldType.BOOLEAN).description("완료 가능 여부"),
                    ),
                ),
            )
    }

    @Test
    fun `심화 질문 스킵`() {
        mockMvc
            .perform(post("/api/v1/retrospectives/{retrospectiveId}/skip", retrospectiveId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "retrospect/skip",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `회고 완료 - AI 요약 생성`() {
        val summary = aiSummaryResponse()
        whenever(retrospectiveRegister.complete(retrospectiveId, userId)).thenReturn(summary)

        mockMvc
            .perform(post("/api/v1/retrospectives/{retrospectiveId}/complete", retrospectiveId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/complete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("AI 생성 제목"),
                        fieldWithPath("data.summary.feedback").type(JsonFieldType.STRING).description("AI 피드백"),
                        fieldWithPath("data.summary.insight").type(JsonFieldType.STRING).description("인사이트"),
                        fieldWithPath("data.summary.doneWork").type(JsonFieldType.STRING).description("한 일"),
                        fieldWithPath("data.summary.blockedPoint").type(JsonFieldType.STRING).description("막힌 지점"),
                        fieldWithPath("data.summary.solutionProcess").type(JsonFieldType.STRING).description("해결 과정"),
                        fieldWithPath("data.summary.lessonLearned").type(JsonFieldType.STRING).description("배운 점"),
                    ),
                ),
            )
    }

    @Test
    fun `심화 질문 조회`() {
        val response =
            DeepQuestionResponse(
                isReady = true,
                content = "비슷한 상황이 또 생긴다면 어떻게 하실 것 같나요?",
            )
        whenever(retrospectiveFinder.findDeepQuestion(retrospectiveId, userId))
            .thenReturn(response)

        mockMvc
            .perform(get("/api/v1/retrospectives/{retrospectiveId}/deep-question", retrospectiveId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/deep-question",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.isReady").type(JsonFieldType.BOOLEAN).description("심화 질문 생성 완료 여부"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("심화 질문 내용").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `회고 저장`() {
        val request = SaveRetrospectiveRequest(title = "오늘의 회고")
        val retro = completedRetrospective()
        whenever(
            retrospectiveRegister.save(
                retrospectiveId = any(),
                userId = any(),
                title = any(),
            ),
        ).thenReturn(retro)

        mockMvc
            .perform(
                post("/api/v1/retrospectives/{retrospectiveId}/save", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/save",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    requestFields(
                        fieldWithPath("title").type(JsonFieldType.STRING).description("회고 제목"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("회고 상태"),
                        fieldWithPath("data.summary").type(JsonFieldType.OBJECT).description("회고 요약").optional(),
                        fieldWithPath("data.summary.feedback").type(JsonFieldType.STRING).description("AI 피드백").optional(),
                        fieldWithPath("data.summary.insight").type(JsonFieldType.STRING).description("인사이트").optional(),
                        fieldWithPath("data.summary.doneWork").type(JsonFieldType.STRING).description("한 일").optional(),
                        fieldWithPath("data.summary.blockedPoint").type(JsonFieldType.STRING).description("막힌 지점").optional(),
                        fieldWithPath("data.summary.solutionProcess").type(JsonFieldType.STRING).description("해결 과정").optional(),
                        fieldWithPath("data.summary.lessonLearned").type(JsonFieldType.STRING).description("배운 점").optional(),
                        fieldWithPath("data.completedAt").type(JsonFieldType.STRING).description("완료 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `회고 제목 수정`() {
        val request = UpdateTitleRequest(title = "수정된 회고 제목")

        mockMvc
            .perform(
                patch("/api/v1/retrospectives/{retrospectiveId}/title", retrospectiveId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "retrospect/update-title",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    requestFields(
                        fieldWithPath("title").type(JsonFieldType.STRING).description("수정할 제목"),
                    ),
                ),
            )
    }

    @Test
    fun `회고 다시 시작`() {
        val retro = retrospectiveWithQ1()
        whenever(retrospectiveRegister.restart(retrospectiveId, userId)).thenReturn(retro)

        mockMvc
            .perform(post("/api/v1/retrospectives/{retrospectiveId}/restart", retrospectiveId))
            .andExpect(status().isCreated)
            .andDo(
                document(
                    "retrospect/restart",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.retrospectiveId").type(JsonFieldType.STRING).description("새 회고 ID"),
                        fieldWithPath("data.firstQuestionType").type(JsonFieldType.STRING).description("첫 번째 질문 타입"),
                        fieldWithPath("data.firstQuestionContent").type(JsonFieldType.STRING).description("첫 번째 질문 내용"),
                    ),
                ),
            )
    }

    @Test
    fun `회고 삭제`() {
        mockMvc
            .perform(delete("/api/v1/retrospectives/{retrospectiveId}", retrospectiveId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "retrospect/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `회고 목록 조회`() {
        val retros = listOf(completedRetrospective())
        whenever(retrospectiveFinder.findAllByUserId(userId)).thenReturn(retros)

        mockMvc
            .perform(get("/api/v1/retrospectives"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/find-all",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data[].feedback")
                            .type(JsonFieldType.STRING)
                            .description("AI 피드백 한 줄")
                            .optional(),
                        fieldWithPath("data[].completedAt").type(JsonFieldType.STRING).description("완료 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `회고 상세 조회`() {
        val retro = completedRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        mockMvc
            .perform(get("/api/v1/retrospectives/{retrospectiveId}", retrospectiveId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/find-by-id",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("회고 상태"),
                        fieldWithPath("data.summary").type(JsonFieldType.OBJECT).description("회고 요약").optional(),
                        fieldWithPath("data.summary.feedback")
                            .type(JsonFieldType.STRING)
                            .description("AI 피드백")
                            .optional(),
                        fieldWithPath("data.summary.insight").type(JsonFieldType.STRING).description("인사이트").optional(),
                        fieldWithPath("data.summary.doneWork").type(JsonFieldType.STRING).description("한 일").optional(),
                        fieldWithPath("data.summary.blockedPoint")
                            .type(JsonFieldType.STRING)
                            .description("막힌 지점")
                            .optional(),
                        fieldWithPath("data.summary.solutionProcess")
                            .type(JsonFieldType.STRING)
                            .description("해결 과정")
                            .optional(),
                        fieldWithPath("data.summary.lessonLearned")
                            .type(JsonFieldType.STRING)
                            .description("배운 점")
                            .optional(),
                        fieldWithPath("data.completedAt").type(JsonFieldType.STRING).description("완료 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `회고 나가기`() {
        mockMvc
            .perform(post("/api/v1/retrospectives/{retrospectiveId}/exit", retrospectiveId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "retrospect/exit",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `월별 캘린더 조회`() {
        val retro = completedRetrospective()
        whenever(retrospectiveFinder.findByUserIdAndYearMonth(userId, 2026, 3))
            .thenReturn(listOf(retro))

        mockMvc
            .perform(
                get("/api/v1/retrospectives/calendar")
                    .param("year", "2026")
                    .param("month", "3"),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/calendar",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("year").description("년도"),
                        parameterWithName("month").description("월"),
                    ),
                    responseFields(
                        fieldWithPath("data.year").type(JsonFieldType.NUMBER).description("년도"),
                        fieldWithPath("data.month").type(JsonFieldType.NUMBER).description("월"),
                        fieldWithPath("data.days").type(JsonFieldType.ARRAY).description("회고 작성 날짜 목록"),
                        fieldWithPath("data.days[].date").type(JsonFieldType.STRING).description("날짜").optional(),
                        fieldWithPath("data.days[].count").type(JsonFieldType.NUMBER).description("회고 횟수").optional(),
                        fieldWithPath("data.weeklyCount").type(JsonFieldType.NUMBER).description("이번 주 회고 횟수"),
                        fieldWithPath("data.isWeeklyGoalAchieved")
                            .type(JsonFieldType.BOOLEAN)
                            .description("주간 목표 달성 여부 (3회 이상)"),
                    ),
                ),
            )
    }

    @Test
    fun `날짜별 회고 목록 조회`() {
        val retro = completedRetrospective()
        whenever(retrospectiveFinder.findByUserIdAndDate(userId, LocalDate.of(2026, 3, 10)))
            .thenReturn(listOf(retro))

        mockMvc
            .perform(
                get("/api/v1/retrospectives/calendar/daily")
                    .param("date", "2026-03-10"),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/calendar-daily",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("date").description("날짜 (yyyy-MM-dd)"),
                    ),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data[].feedback").type(JsonFieldType.STRING).description("AI 피드백").optional(),
                        fieldWithPath("data[].completedAt").type(JsonFieldType.STRING).description("완료 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `회고 제목 검색`() {
        val keyword = "로그인"
        val retros =
            listOf(
                Retrospective.create(userId).apply {
                    title = "로그인 API 연동 회고"
                    completedAt = LocalDateTime.now()
                    summary =
                        RetrospectiveSummary(
                            feedback = "오늘 작업을 잘 마무리했어요.",
                            insight = "문제를 작게 나누는 것이 중요합니다.",
                            doneWork = "로그인 API 연동 완료",
                            blockedPoint = "토큰 만료 처리 어려움",
                            solutionProcess = "공식 문서 참고",
                            lessonLearned = "초반에 에러 처리 설계",
                        )
                },
                Retrospective.create(userId).apply {
                    title = "로그인 버그 수정 회고"
                    completedAt = LocalDateTime.now()
                    summary =
                        RetrospectiveSummary(
                            feedback = "오늘도 열심히 했습니다.",
                            insight = "작게 나누는 습관 필요",
                            doneWork = "로그인 버그 수정 완료",
                            blockedPoint = "세션 처리 어려움",
                            solutionProcess = "팀 코드 리뷰 참고",
                            lessonLearned = "테스트 먼저 작성",
                        )
                },
            )
        whenever(retrospectiveFinder.searchByTitle(userId, keyword)).thenReturn(retros)

        mockMvc
            .perform(
                get("/api/v1/retrospectives/search")
                    .param("keyword", keyword),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/search",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    queryParameters(
                        parameterWithName("keyword").description("검색 키워드 (예시: 로그인)"),
                    ),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data[].feedback").type(JsonFieldType.STRING).description("AI 피드백 한 줄").optional(),
                        fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성 시간").optional(),
                    ),
                ),
            )
    }

    @Test
    fun `최근 검색 기록 조회`() {
        val histories =
            listOf(
                SearchHistory.create(UUID.randomUUID(), "회고"),
                SearchHistory.create(UUID.randomUUID(), "테스트"),
            )

        whenever(searchHistoryFinder.findRecent(any()))
            .thenReturn(histories)

        mockMvc
            .perform(get("/api/v1/retrospectives/search/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].keyword").value("회고"))
            .andExpect(jsonPath("$.data[1].keyword").value("테스트"))
            .andDo(
                document(
                    "retrospect/search-info",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].keyword").type(JsonFieldType.STRING).description("검색 키워드 리스트"),
                    ),
                ),
            )
    }

    @Test
    fun `최근 검색 기록 조회 - 데이터 없음`() {
        whenever(searchHistoryFinder.findRecent(userId))
            .thenReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/retrospectives/search/info"))
            .andExpect(status().isOk)
    }
}
