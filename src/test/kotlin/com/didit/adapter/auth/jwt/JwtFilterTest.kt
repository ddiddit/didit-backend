package com.didit.adapter.auth.jwt

import com.didit.domain.auth.enums.Role
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtFilterTest {
    private lateinit var jwtProvider: JwtProvider
    private lateinit var jwtFilter: JwtFilter

    private val secret = "jwt-provider-test-key-jwt-provider-test-key"
    private val accessExp = 3600000L
    private val refreshExp = 1209600000L

    @BeforeEach
    fun setup() {
        jwtProvider = JwtProvider(secret, accessExp, refreshExp)
        jwtFilter = JwtFilter(jwtProvider)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `JWT 인증 성공 시 SecurityContext에 인증정보 저장`() {
        val token = jwtProvider.createAccessToken(UUID.randomUUID(), Role.USER)

        val request: HttpServletRequest =
            mock {
                on { getHeader("Authorization") } doReturn "Bearer $token"
            }
        val response: HttpServletResponse = mock()
        val chain: FilterChain = mock()

        jwtFilter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)

        val roles = auth.authorities.map { it.authority }
        assertEquals(listOf("ROLE_${Role.USER.name}"), roles)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `JWT 없으면 SecurityContext는 null`() {
        val request: HttpServletRequest =
            mock {
                on { getHeader("Authorization") } doReturn null
            }
        val response: HttpServletResponse = mock()
        val chain: FilterChain = mock()

        jwtFilter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNull(auth)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `JWT 잘못된 토큰이면 SecurityContext는 null`() {
        val request: HttpServletRequest =
            mock {
                on { getHeader("Authorization") } doReturn "Bearer invalid.token.value"
            }
        val response: HttpServletResponse = mock()
        val chain: FilterChain = mock()

        jwtFilter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNull(auth)
        verify(chain).doFilter(request, response)
    }
}
