package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.inquiry.dto.InquiryAnswerRequest
import com.didit.application.inquiry.provided.InquiryFinder
import com.didit.application.inquiry.provided.InquiryModifier
import com.didit.docs.AdminAuthenticatedRestDocsSupport
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.inquiry.InquiryType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class InquiryAdminApiTest : AdminAuthenticatedRestDocsSupport() {
    private val inquiryModifier: InquiryModifier = mock(InquiryModifier::class.java)
    private val inquiryFinder: InquiryFinder = mock(InquiryFinder::class.java)

    override fun initController() = InquiryAdminApi(inquiryModifier, inquiryFinder)

    private fun createInquiry(): Inquiry =
        Inquiry(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            email = "test@test.com",
            type = InquiryType.USAGE,
            typeEtc = "",
            content = "문의 내용입니다.",
            isAgreed = true,
            status = InquiryStatus.ANSWERED,
            adminId = adminId,
            adminAnswer = "기존 답변",
            answeredAt = LocalDateTime.now(),
        ).apply {
            val now = LocalDateTime.now()
            ReflectionTestUtils.setField(this, "createdAt", now)
            ReflectionTestUtils.setField(this, "updatedAt", now)
        }

    @Test
    fun `관리자 문의 답변 등록`() {
        val inquiryId = UUID.randomUUID()

        val request = InquiryAnswerRequest("답변입니다.")
        val inquiry = createInquiry()

        whenever(inquiryModifier.answer(any(), any(), any()))
            .thenReturn(inquiry)

        mockMvc
            .perform(
                post("/api/v1/admin/inquiries/{inquiryId}", inquiryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "inquiry/admin/answer",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("inquiryId").description("문의 ID"),
                    ),
                    requestFields(
                        fieldWithPath("answer")
                            .type(JsonFieldType.STRING)
                            .description("답변 내용"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문의 ID"),
                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("문의자 이메일"),
                        fieldWithPath("data.type")
                            .type(JsonFieldType.STRING)
                            .description("문의 유형(USAGE, BUG, IMPROVEMENT, ETC)"),
                        fieldWithPath("data.typeEtc").type(JsonFieldType.STRING).optional().description("기타 유형 내용"),
                        fieldWithPath("data.content").type(JsonFieldType.STRING).description("문의 내용"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("문의 상태(PENDING, ANSWERED)"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("문의 생성 시간"),
                        fieldWithPath("data.adminAnswer").type(JsonFieldType.STRING).description("관리자 답변"),
                        fieldWithPath("data.answeredAt").type(JsonFieldType.STRING).description("답변 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 문의 답변 수정`() {
        val inquiryId = UUID.randomUUID()

        val request = InquiryAnswerRequest("수정된 답변입니다.")
        val inquiry = createInquiry()

        whenever(inquiryModifier.updateAnswer(any(), any(), any()))
            .thenReturn(inquiry)

        mockMvc
            .perform(
                patch("/api/v1/admin/inquiries/{inquiryId}", inquiryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "inquiry/admin/answer-update",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("inquiryId").description("문의 ID"),
                    ),
                    requestFields(
                        fieldWithPath("answer")
                            .type(JsonFieldType.STRING)
                            .description("수정할 답변 내용"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문의 ID"),
                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("문의자 이메일"),
                        fieldWithPath("data.type")
                            .type(JsonFieldType.STRING)
                            .description("문의 유형(USAGE, BUG, IMPROVEMENT, ETC)"),
                        fieldWithPath("data.typeEtc").type(JsonFieldType.STRING).optional().description("기타 유형 내용"),
                        fieldWithPath("data.content")
                            .type(JsonFieldType.STRING)
                            .description("문의 내용"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("문의 상태(PENDING, ANSWERED)"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("문의 생성 시간"),
                        fieldWithPath("data.adminAnswer").type(JsonFieldType.STRING).description("관리자 답변"),
                        fieldWithPath("data.answeredAt").type(JsonFieldType.STRING).description("답변 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 문의 목록 조회`() {
        val inquiries = listOf(createInquiry(), createInquiry())

        whenever(inquiryFinder.findAll())
            .thenReturn(inquiries)

        mockMvc
            .perform(
                get("/api/v1/admin/inquiries")
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "inquiry/admin/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].id").type(JsonFieldType.STRING).description("문의 ID"),
                        fieldWithPath("data[].email").type(JsonFieldType.STRING).description("문의자 이메일"),
                        fieldWithPath("data[].type")
                            .type(JsonFieldType.STRING)
                            .description("문의 유형(USAGE, BUG, IMPROVEMENT, ETC)"),
                        fieldWithPath("data[].content")
                            .type(JsonFieldType.STRING)
                            .description("문의 내용"),
                        fieldWithPath("data[].status")
                            .type(JsonFieldType.STRING)
                            .description("문의 상태(PENDING, ANSWERED)"),
                        fieldWithPath("data[].createdAt")
                            .type(JsonFieldType.STRING)
                            .description("생성 시간"),
                    ),
                ),
            )
    }

    @Test
    fun `관리자 문의 상세 조회`() {
        val inquiryId = UUID.randomUUID()
        val inquiry = createInquiry()

        whenever(inquiryFinder.findById(any()))
            .thenReturn(inquiry)

        mockMvc
            .perform(
                get("/api/v1/admin/inquiries/{inquiryId}", inquiryId)
                    .accept(MediaType.APPLICATION_JSON),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "inquiry/admin/detail",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    pathParameters(
                        parameterWithName("inquiryId").description("문의 ID"),
                    ),
                    responseFields(
                        fieldWithPath("data.id").type(JsonFieldType.STRING).description("문의 ID"),
                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("문의자 이메일"),
                        fieldWithPath("data.type")
                            .type(JsonFieldType.STRING)
                            .description("문의 유형(USAGE, BUG, IMPROVEMENT, ETC)"),
                        fieldWithPath("data.typeEtc")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("기타 유형 내용"),
                        fieldWithPath("data.content")
                            .type(JsonFieldType.STRING)
                            .description("문의 내용"),
                        fieldWithPath("data.status")
                            .type(JsonFieldType.STRING)
                            .description("문의 상태(PENDING, ANSWERED)"),
                        fieldWithPath("data.createdAt")
                            .type(JsonFieldType.STRING)
                            .description("문의 생성 시간"),
                        fieldWithPath("data.adminAnswer")
                            .type(JsonFieldType.STRING)
                            .description("관리자 답변"),
                        fieldWithPath("data.answeredAt")
                            .type(JsonFieldType.STRING)
                            .description("답변 시간"),
                    ),
                ),
            )
    }
}
