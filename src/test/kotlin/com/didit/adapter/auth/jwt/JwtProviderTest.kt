package com.didit.adapter.auth.jwt

import com.didit.domain.auth.enums.Role
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class JwtProviderTest {
    private lateinit var jwtProvider: JwtProvider
    private val secret = "jwt-provider-test-key-jwt-provider-test-key"
    private val accessExp = 3600000L
    private val refreshExp = 1209600000L

    @BeforeEach
    fun setup() {
        jwtProvider = JwtProvider(secret, accessExp, refreshExp)
    }

    @Test
    fun `AccessToken_생성_및_검증`() {
        val userId = UUID.randomUUID()
        val role = Role.USER

        val token = jwtProvider.createAccessToken(userId, role)

        assertTrue(jwtProvider.validateToken(token))
        assertEquals(userId, jwtProvider.getUserId(token))
        assertEquals(role, jwtProvider.getRole(token))
    }

    @Test
    fun `RefreshToken_생성_밎_검증`() {
        val userId = UUID.randomUUID()
        val token = jwtProvider.createRefreshToken(userId)

        assertTrue(jwtProvider.validateToken(token))
        assertEquals(userId, jwtProvider.getUserId(token))
    }
}
