package com.didit.adapter.webapi.retrospect

import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.organization.Project
import com.didit.domain.organization.Tag
import com.didit.support.RetrospectiveFixture
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.Test

class RetrospectV2ApiTest : AuthenticatedRestDocsSupport() {
    private val retrospectiveFinder: RetrospectiveFinder = mock(RetrospectiveFinder::class.java)

    override fun initController() = RetrospectV2Api(retrospectiveFinder)

    private val retrospectiveId = UUID.randomUUID()

    @Test
    fun `회고 상세 조회 v2`() {
        val result =
            RetrospectiveDetailResult(
                retrospective = completedRetrospective(),
                project = project(),
                tags = listOf(tag1(), tag2()),
            )

        whenever(
            retrospectiveFinder.findRetrospectWithProjectAndTags(retrospectiveId, userId),
        ).thenReturn(result)

        mockMvc
            .perform(get("/api/v2/retrospectives/{retrospectiveId}", retrospectiveId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "retrospect/v2/find-by-id",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("retrospectiveId").description("회고 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data.title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("회고 상태"),
                        fieldWithPath("data.content").type(JsonFieldType.OBJECT).description("회고 내용").optional(),
                        fieldWithPath("data.content.summary")
                            .type(JsonFieldType.STRING)
                            .description("회고 요약")
                            .optional(),
                        fieldWithPath("data.content.feedback")
                            .type(JsonFieldType.STRING)
                            .description("AI 피드백")
                            .optional(),
                        fieldWithPath("data.content.insight").type(JsonFieldType.STRING).description("인사이트").optional(),
                        fieldWithPath("data.content.doneWork").type(JsonFieldType.STRING).description("한 일").optional(),
                        fieldWithPath("data.content.blockedPoint")
                            .type(JsonFieldType.ARRAY)
                            .description("막힌 지점")
                            .optional(),
                        fieldWithPath("data.content.solutionProcess")
                            .type(JsonFieldType.ARRAY)
                            .description("해결 과정")
                            .optional(),
                        fieldWithPath("data.content.lessonLearned")
                            .type(JsonFieldType.ARRAY)
                            .description("배운 점")
                            .optional(),
                        fieldWithPath("data.content.nextAction")
                            .type(JsonFieldType.ARRAY)
                            .description("다음 액션")
                            .optional(),
                        fieldWithPath("data.completedAt").type(JsonFieldType.STRING).description("완료 시간").optional(),
                        fieldWithPath("data.project").type(JsonFieldType.OBJECT).description("프로젝트 정보").optional(),
                        fieldWithPath("data.project.id").type(JsonFieldType.STRING).description("프로젝트 ID").optional(),
                        fieldWithPath("data.project.name").type(JsonFieldType.STRING).description("프로젝트 이름").optional(),
                        fieldWithPath("data.tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                        fieldWithPath("data.tags[].id").type(JsonFieldType.STRING).description("태그 ID"),
                        fieldWithPath("data.tags[].name").type(JsonFieldType.STRING).description("태그 이름"),
                    ),
                ),
            )
    }

    private fun completedRetrospective() = RetrospectiveFixture.createCompleted(userId)

    private fun project() =
        Project(
            id = UUID.randomUUID(),
            userId = userId,
            name = "프로젝트 이름",
        )

    private fun tag1() =
        Tag(
            id = UUID.randomUUID(),
            userId = userId,
            name = "태그1",
        )

    private fun tag2() =
        Tag(
            id = UUID.randomUUID(),
            userId = userId,
            name = "태그2",
        )
}
