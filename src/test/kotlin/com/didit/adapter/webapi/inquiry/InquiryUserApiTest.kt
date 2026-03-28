package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.inquiry.dto.InquiryRequest
import com.didit.application.inquiry.provided.InquiryInfoFinder
import com.didit.application.inquiry.provided.InquiryRegister
import com.didit.docs.ApiDocumentUtils
import com.didit.docs.AuthenticatedRestDocsSupport
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
import java.util.UUID

class InquiryUserApiTest : AuthenticatedRestDocsSupport() {
    private val inquiryInfoFinder: InquiryInfoFinder = mock(InquiryInfoFinder::class.java)
    private val inquiryRegister: InquiryRegister = mock(InquiryRegister::class.java)

    override fun initController() = InquiryUserApi(inquiryInfoFinder, inquiryRegister)

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

        whenever(inquiryRegister.register(any()))
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
}
