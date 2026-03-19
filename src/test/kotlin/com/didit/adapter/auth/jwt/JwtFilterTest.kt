package com.didit.adapter.auth.jwt

import com.didit.adapter.webapi.test.TestController
import com.didit.domain.auth.enums.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(TestController::class)
@Import(
    JwtFilterTest.TestSecurityConfig::class,
    JwtFilterTest.TestConfig::class,
)
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
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `JWT_권한_없는_경우_인증_실패`() {
        mvc
            .get("/test/protected") {
            }.andExpect {
                status { isForbidden() }
            }
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun jwtProvider(): JwtProvider =
            JwtProvider(
                "test-secret-key-test-secret-key-test-secret-key",
                3600000L,
                1209600000L,
            )
    }

    @TestConfiguration
    class TestSecurityConfig {
        @Bean
        fun securityFilterChain(
            http: HttpSecurity,
            jwtProvider: JwtProvider,
        ): SecurityFilterChain =
            http
                .csrf { it.disable() }
                .authorizeHttpRequests {
                    it.requestMatchers("/test/protected").authenticated()
                    it.anyRequest().permitAll()
                }.addFilterBefore(
                    JwtFilter(jwtProvider),
                    UsernamePasswordAuthenticationFilter::class.java,
                ).build()
    }
}
