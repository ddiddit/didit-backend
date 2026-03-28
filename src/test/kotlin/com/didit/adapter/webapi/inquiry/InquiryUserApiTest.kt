package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.inquiry.dto.InquiryRequest
import com.didit.application.inquiry.provided.InquiryFinder
import com.didit.application.inquiry.provided.InquiryInfoFinder
import com.didit.application.inquiry.provided.InquiryRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

class InquiryUserApiTest : AuthenticatedRestDocsSupport() {
    private val inquiryInfoFinder: InquiryInfoFinder = mock(InquiryInfoFinder::class.java)
    private val inquiryRegister: InquiryRegister = mock(InquiryRegister::class.java)
    private val inquiryFinder: InquiryFinder = mock(InquiryFinder::class.java)

    override fun initController() = InquiryUserApi(inquiryInfoFinder, inquiryRegister, inquiryFinder)

    @Test
    @WithMockUser
    fun `문의 접근 시 사용자 이메일 반환`() {
        val userId = UUID.randomUUID()

        whenever(inquiryInfoFinder.findEmail(any()))
            .thenReturn("test@email.com")

        mockMvc
            .perform(
                get("/api/v1/inquiry"),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "inquiry/user/info",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data")
                            .type(JsonFieldType.STRING)
                            .description("사용자 이메일"),
                    ),
                ),
            )
    }

    @Test
    @WithMockUser
    fun `문의 등록`() {
        val request =
            InquiryRequest(
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용입니다.",
                isAgreed = true,
            )

        whenever(inquiryRegister.register(any(), any()))
            .thenReturn(mock())

        mockMvc
            .perform(
                post("/api/v1/inquiry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNoContent)
            .andDo(
                document(
                    "inquiry/user/register",
                    ApiDocumentUtils.getDocumentRequest(),
                    requestFields(
                        fieldWithPath("type")
                            .type(JsonFieldType.STRING)
                            .description("문의 유형 (USAGE, BUG, IMPROVEMENT, ETC)"),
                        fieldWithPath("typeEtc")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("기타 유형 입력 값"),
                        fieldWithPath("content")
                            .type(JsonFieldType.STRING)
                            .description("문의 내용"),
                        fieldWithPath("isAgreed")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인정보 수집 동의 여부"),
                    ),
                ),
            )
    }

    @Test
    @WithMockUser
    fun `문의 내역 리스트 조회`() {
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now()

        val inquiries =
            listOf(
                Inquiry(
                    id = UUID.randomUUID(),
                    userId = userId,
                    email = "test@email.com",
                    type = InquiryType.USAGE,
                    typeEtc = null,
                    content = "문의 내용1",
                    isAgreed = true,
                    status = InquiryStatus.PENDING,
                    adminAnswer = null,
                    adminId = null,
                    answeredAt = null,
                ).withCreatedAt(now),
                Inquiry(
                    id = UUID.randomUUID(),
                    userId = userId,
                    email = "test@email.com",
                    type = InquiryType.BUG,
                    typeEtc = null,
                    content = "문의 내용2",
                    isAgreed = true,
                    status = InquiryStatus.ANSWERED,
                    adminAnswer = "답변입니다.",
                    adminId = UUID.randomUUID(),
                    answeredAt = LocalDateTime.now(),
                ).withCreatedAt(now),
            )

        whenever(inquiryFinder.findAll(any()))
            .thenReturn(inquiries)

        mockMvc
            .perform(
                get("/api/v1/inquiry/list"),
            ).andExpect(status().isOk)
            .andDo(
                document(
                    "inquiry/user/list",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data[].type")
                            .type(JsonFieldType.STRING)
                            .description("문의 유형"),
                        fieldWithPath("data[].content")
                            .type(JsonFieldType.STRING)
                            .description("문의 내용"),
                        fieldWithPath("data[].status")
                            .type(JsonFieldType.STRING)
                            .description("답변 상태"),
                        fieldWithPath("data[].adminAnswer")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("관리자 답변"),
                        fieldWithPath("data[].createdAt")
                            .type(JsonFieldType.STRING)
                            .description("생성일"),
                    ),
                ),
            )
    }

    @Test
    fun `문의 내역 없을 때`() {
        whenever(inquiryFinder.findAll(any()))
            .thenReturn(emptyList())

        mockMvc
            .perform(get("/api/v1/inquiry/list"))
            .andExpect(status().isOk)
    }

    private fun Inquiry.withCreatedAt(time: LocalDateTime): Inquiry {
        val field = this::class.java.superclass.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(this, time)
        return this
    }
}
