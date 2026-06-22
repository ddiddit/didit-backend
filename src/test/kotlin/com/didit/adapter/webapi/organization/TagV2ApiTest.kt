package com.didit.adapter.webapi.organization

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

class TagV2ApiTest : AuthenticatedRestDocsSupport() {
    private val retrospectiveFinder: RetrospectiveFinder = mock(RetrospectiveFinder::class.java)

    override fun initController() = TagV2Api(retrospectiveFinder)

    @Test
    fun `태그 기준 회고 목록 조회 v2`() {
        val tagId = UUID.randomUUID()

        val results =
            listOf(
                RetrospectiveDetailResult(
                    retrospective = RetrospectiveFixture.createCompleted(userId),
                    project = project(),
                    tags = listOf(tag(tagId, "태그1"), tag(UUID.randomUUID(), "태그2")),
                ),
            )

        whenever(retrospectiveFinder.findByTagIdWithProjectAndTags(userId, tagId)).thenReturn(results)

        mockMvc
            .perform(get("/api/v2/tags/{tagId}", tagId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "tag/v2/retrospect-list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("tagId").description("조회할 태그 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("회고 제목").optional(),
                        fieldWithPath("data[].summary").type(JsonFieldType.STRING).description("회고 요약").optional(),
                        fieldWithPath("data[].completedAt")
                            .type(JsonFieldType.STRING)
                            .description("완료 시간")
                            .optional(),
                        fieldWithPath("data[].projectName")
                            .type(JsonFieldType.STRING)
                            .description("프로젝트 이름")
                            .optional(),
                        fieldWithPath("data[].tags").type(JsonFieldType.ARRAY).description("태그 목록"),
                        fieldWithPath("data[].tags[].id").type(JsonFieldType.STRING).description("태그 ID"),
                        fieldWithPath("data[].tags[].name").type(JsonFieldType.STRING).description("태그 이름"),
                    ),
                ),
            )
    }

    private fun project() =
        Project(
            id = UUID.randomUUID(),
            userId = userId,
            name = "프로젝트 이름",
        )

    private fun tag(
        tagId: UUID,
        name: String,
    ) = Tag(
        id = tagId,
        userId = userId,
        name = name,
    )
}
