package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.organization.dto.TagCreateRequest
import com.didit.application.organization.provided.RetrospectTagFinder
import com.didit.application.organization.provided.TagFinder
import com.didit.application.organization.provided.TagModifier
import com.didit.application.organization.provided.TagRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
import com.didit.domain.organization.Tag
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class TagApiTest : AuthenticatedRestDocsSupport() {
    private val tagRegister: TagRegister = org.mockito.Mockito.mock(TagRegister::class.java)
    private val tagFinder: TagFinder = org.mockito.Mockito.mock(TagFinder::class.java)
    private val tagModifier: TagModifier = org.mockito.Mockito.mock(TagModifier::class.java)
    private val retrospectTagFinder: RetrospectTagFinder = org.mockito.Mockito.mock(RetrospectTagFinder::class.java)

    override fun initController() = TagApi(tagRegister, tagFinder, tagModifier, retrospectTagFinder)

    @Test
    fun `태그 생성`() {
        val request = TagCreateRequest(name = "테스트 태그")
        val tag = Tag.create(userId = UUID.randomUUID(), name = request.name)

        whenever(tagRegister.create(any(), any())).thenReturn(tag)

        mockMvc
            .perform(
                post("/api/v1/tags")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "tag/create",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    requestFields(
                        fieldWithPath("name").type(JsonFieldType.STRING).description("태그 이름 (최대 15자)"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("태그 ID"),
                        fieldWithPath("data.name").type(JsonFieldType.STRING).description("태그 이름"),
                    ),
                ),
            )
    }

    @Test
    fun `사용자 태그 목록 조회`() {
        val tags =
            listOf(
                Tag.create(userId = UUID.randomUUID(), name = "태그1"),
                Tag.create(userId = UUID.randomUUID(), name = "태그2"),
            )

        whenever(tagFinder.findAllByUserId(any())).thenReturn(tags)

        mockMvc
            .perform(get("/api/v1/tags"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "tag/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("태그 ID"),
                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("태그 이름"),
                    ),
                ),
            )
    }

    @Test
    fun `태그 삭제`() {
        doNothing().whenever(tagModifier).delete(any(), any())

        val tagId = UUID.randomUUID()
        mockMvc
            .perform(delete("/api/v1/tags/{tagId}", tagId))
            .andExpect(status().isNoContent)
            .andDo(
                document(
                    "tag/delete",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        org.springframework.restdocs.request.RequestDocumentation
                            .parameterWithName("tagId")
                            .description("삭제할 태그 ID"),
                    ),
                ),
            )
    }

    @Test
    fun `태그로 회고 목록 조회`() {
        val tagId = UUID.randomUUID()

        val retro1 =
            Retrospective.create(userId).apply {
                title = "첫 번째 회고"
                completedAt = LocalDateTime.now()
                summary =
                    RetrospectiveSummary(
                        summary = "첫 번째 회고 요약입니다.",
                        feedback = "피드백 1",
                        insight = "인사이트 1",
                        doneWork = "작업 1",
                        blockedPoint = "문제 1",
                        solutionProcess = "해결 1",
                        lessonLearned = "배운 점 1",
                        nextAction = "다음 액션 1",
                    )
            }

        val retro2 =
            Retrospective.create(userId).apply {
                title = "두 번째 회고"
                completedAt = LocalDateTime.now()
                summary =
                    RetrospectiveSummary(
                        summary = "두 번째 회고 요약입니다.",
                        feedback = "피드백 2",
                        insight = "인사이트 2",
                        doneWork = "작업 2",
                        blockedPoint = "문제 2",
                        solutionProcess = "해결 2",
                        lessonLearned = "배운 점 2",
                        nextAction = "다음 액션 2",
                    )
            }

        whenever(retrospectTagFinder.findAllByTagId(tagId))
            .thenReturn(listOf(retro1, retro2))

        mockMvc
            .perform(get("/api/v1/tags/{tagId}", tagId))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "tag/retrospect-list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("tagId").description("조회할 태그 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("회고 ID"),
                        fieldWithPath("data[].title")
                            .type(JsonFieldType.STRING)
                            .description("회고 제목")
                            .optional(),
                        fieldWithPath("data[].summary")
                            .type(JsonFieldType.STRING)
                            .description("회고 요약")
                            .optional(),
                        fieldWithPath("data[].completedAt")
                            .type(JsonFieldType.STRING)
                            .description("완료 시간")
                            .optional(),
                    ),
                ),
            )
    }
}
