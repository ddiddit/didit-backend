package com.didit.adapter.webapi.users

import com.didit.adapter.auth.security.CustomUserDetails
import com.didit.application.users.provided.WithdrawUseCase
import com.didit.docs.ApiDocumentUtils
import com.didit.domain.auth.enums.Role
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(UsersController::class)
@AutoConfigureRestDocs
@Import(UsersControllerTestConfig::class)
class UsersControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val withdrawUseCase: WithdrawUseCase,
) {
    @Test
    fun `회원_탈퇴_성공`() {
        val user =
            CustomUserDetails(
                userId = UUID.randomUUID(),
                role = Role.USER,
            )

        doNothing().`when`(withdrawUseCase).execute(any())

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .with(csrf())
                    .with(user(user)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isEmpty)
            .andDo(
                document(
                    "users/withdraw",
                    ApiDocumentUtils.getDocumentRequest(),
                    ApiDocumentUtils.getDocumentResponse(),
                    responseFields(
                        fieldWithPath("data")
                            .type(JsonFieldType.NULL)
                            .description("응답 데이터 (회원 탈퇴 성공 시 빈 객체 반환)"),
                    ),
                ),
            )
    }
}
