package com.didit.docs

import com.didit.support.TestController
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ApiDocsTest : RestDocsSupport() {
    override fun initController() = TestController()

    @Test
    fun `데이터 성공 응답 문서화`() {
        mockMvc
            .perform(get("/test/success-data"))
            .andExpect(status().isOk)
            .andDo(
                document(
                    "response/success-data",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        *CommonDocumentation.successResponseFields(
                            fieldWithPath("data.id").type(JsonFieldType.STRING).description("ID"),
                            fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                        ),
                    ),
                ),
            )
    }

    @Test
    fun `비즈니스 예외 응답 문서화`() {
        mockMvc
            .perform(get("/test/business-error"))
            .andExpect(status().isBadRequest)
            .andDo(
                document(
                    "error/business",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(*CommonDocumentation.errorResponseFields()),
                ),
            )
    }

    @Test
    fun `유효성 검증 실패 응답 문서화`() {
        mockMvc
            .perform(
                post("/test/validation-error")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": ""}"""),
            ).andExpect(status().isBadRequest)
            .andDo(
                document(
                    "error/validation",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(*CommonDocumentation.errorResponseFields()),
                ),
            )
    }

    @Test
    fun `서버 에러 응답 문서화`() {
        mockMvc
            .perform(get("/test/server-error"))
            .andExpect(status().isInternalServerError)
            .andDo(
                document(
                    "error/server",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(*CommonDocumentation.errorResponseFields()),
                ),
            )
    }
}
