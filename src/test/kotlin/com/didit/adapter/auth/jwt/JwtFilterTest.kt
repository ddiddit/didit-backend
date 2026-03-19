package com.didit.adapter.auth.jwt

import com.didit.adapter.config.SecurityConfig
import com.didit.adapter.webapi.test.TestController
import com.didit.domain.auth.enums.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(TestController::class)
@Import(SecurityConfig::class, JwtProvider::class)
class JwtFilterTest {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var jwtProvider: JwtProvider

    @Test
    fun `JWT_인증_성공_테스트`() {
        val userId = UUID.randomUUID()
        val token = jwtProvider.createAccessToken(userId, Role.USER)

        mvc
            .get("/test/protected") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `JWT_권한_없는_경우_인증_실패`() {
        mvc
            .get("/test/protected") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isForbidden() }
            }
    }
}
