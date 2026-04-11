package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.organization.dto.ProjectCreateRequest
import com.didit.adapter.webapi.organization.dto.ProjectOrderRequest
import com.didit.adapter.webapi.organization.dto.UpdateProjectNameRequest
import com.didit.application.organization.provided.ProjectFinder
import com.didit.application.organization.provided.ProjectModifier
import com.didit.application.organization.provided.ProjectRegister
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.organization.Project
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.Test

class ProjectApiTest : AuthenticatedRestDocsSupport() {
    private val projectRegister: ProjectRegister = mock(ProjectRegister::class.java)
    private val projectFinder: ProjectFinder = mock(ProjectFinder::class.java)
    private val projectModifier: ProjectModifier = mock(ProjectModifier::class.java)
    private val retrospectiveFinder: RetrospectiveFinder = mock(RetrospectiveFinder::class.java)

    override fun initController() = ProjectApi(projectRegister, projectFinder, projectModifier, retrospectiveFinder)

    @Test
    fun `프로젝트 생성`() {
        val userId = UUID.randomUUID()

        val request =
            ProjectCreateRequest(
                name = "테스트 프로젝트",
            )

        whenever(projectRegister.create(userId, request.name))
            .thenReturn(
                Project.create(
                    userId = userId,
                    name = request.name,
                ),
            )

        mockMvc
            .perform(
                post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "project/create",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("name")
                            .type(JsonFieldType.STRING)
                            .description("프로젝트 이름 (최대 15자)"),
                    ),
                ),
            )
    }

    @Test
    fun `프로젝트 생성 실패 - 이름 공백`() {
        val request = ProjectCreateRequest(name = "")

        mockMvc
            .perform(
                post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("Authorization", "Bearer access-token"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `프로젝트 목록 조회`() {
        val projects =
            listOf(
                Project.create(
                    userId = UUID.randomUUID(),
                    name = "프로젝트1",
                ),
                Project.create(
                    userId = UUID.randomUUID(),
                    name = "프로젝트2",
                ),
            )

        whenever(projectFinder.findAllByUserId(any()))
            .thenReturn(projects)

        mockMvc
            .perform(
                get("/api/v1/projects"),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "project/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("프로젝트 ID"),
                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("프로젝트 이름"),
                    ),
                ),
            )
    }

    @Test
    fun `프로젝트 이름 수정`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val request =
            UpdateProjectNameRequest(
                name = "수정된 프로젝트",
            )

        mockMvc
            .perform(
                patch("/api/v1/projects/{projectId}/name", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "project/update-name",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID"),
                    ),
                    requestFields(
                        fieldWithPath("name")
                            .type(JsonFieldType.STRING)
                            .description("수정할 프로젝트 이름 (최대 15자)"),
                    ),
                ),
            )

        verify(projectModifier).updateName(
            any(),
            eq(projectId),
            eq(request.name),
        )
    }

    @Test
    fun `프로젝트 삭제`() {
        val projectId = UUID.randomUUID()

        mockMvc
            .perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
                    .delete("/api/v1/projects/{projectId}", projectId),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "project/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    org.springframework.restdocs.request.RequestDocumentation.pathParameters(
                        org.springframework.restdocs.request.RequestDocumentation
                            .parameterWithName("projectId")
                            .description("프로젝트 ID"),
                    ),
                ),
            )

        verify(projectModifier).deleteProject(any(), any())
    }

    @Test
    fun `프로젝트 순서 변경`() {
        val projectIds =
            listOf(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
            )

        val request = ProjectOrderRequest(projectIds = projectIds)

        mockMvc
            .perform(
                patch("/api/v1/projects/order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "project/reorder",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("projectIds")
                            .type(JsonFieldType.ARRAY)
                            .description("정렬 순서대로 나열된 프로젝트 ID 리스트"),
                    ),
                ),
            )
    }

    @Test
    fun `프로젝트별 회고 목록 조회`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val retrospectives =
            listOf(
                Retrospective.create(userId).apply {
                    registerProject(projectId)
                    complete("회고1")
                    saveSummary(
                        RetrospectiveSummary(
                            summary = "...",
                            blockedPoint = "...",
                            solutionProcess = "...",
                            lessonLearned = "...",
                            insightTitle = "insight title",
                            insightDescription = "insight description",
                            nextActionTitle = "next title",
                            nextActionDescription = "next description",
                        ),
                    )
                },
                Retrospective.create(userId).apply {
                    registerProject(projectId)
                    complete("회고2")
                    saveSummary(
                        RetrospectiveSummary(
                            summary = "...",
                            blockedPoint = "...",
                            solutionProcess = "...",
                            lessonLearned = "...",
                            insightTitle = "insight title",
                            insightDescription = "insight description",
                            nextActionTitle = "next title",
                            nextActionDescription = "next description",
                        ),
                    )
                },
            )

        whenever(retrospectiveFinder.findByProject(any(), any()))
            .thenReturn(retrospectives)

        mockMvc
            .perform(
                get("/api/v1/projects/{projectId}", projectId),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "project/retrospect-list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("projectId").description("프로젝트 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data[].title").type(JsonFieldType.STRING).description("회고 제목"),
                        fieldWithPath("data[].summary").type(JsonFieldType.STRING).description("회고 요약"),
                        fieldWithPath("data[].completedAt").type(JsonFieldType.STRING).description("완료일"),
                    ),
                ),
            )
    }
}
