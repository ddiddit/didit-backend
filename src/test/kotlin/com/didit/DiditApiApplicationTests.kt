package com.didit

import com.didit.adapter.auth.jwt.JwtProvider
import com.didit.adapter.auth.social.oidc.AppleOidcVerifier
import com.didit.adapter.auth.social.oidc.GoogleOidcVerifier
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [DiditApiApplicationTests.TestBeans::class],
)
@ActiveProfiles("test")
class DiditApiApplicationTests {
    @TestConfiguration
    class TestBeans {
        @Bean
        fun jwtProvider(): JwtProvider {
            val key = Keys.secretKeyFor(SignatureAlgorithm.HS256)
            return JwtProvider(
                jwtSecret = key.encoded.toString(Charsets.UTF_8),
                accessTokenExpiration = 6000,
                refreshTokenExpiration = 6000,
            )
        }

        @Bean
        fun appleOidcVerifier(): AppleOidcVerifier = mock(AppleOidcVerifier::class.java)

        @Bean
        fun googleOidcVerifier(): GoogleOidcVerifier = mock(GoogleOidcVerifier::class.java)
    }

    @Test
    fun contextLoads() {
    }
}
