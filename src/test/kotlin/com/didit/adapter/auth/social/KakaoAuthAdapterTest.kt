package com.didit.adapter.auth.social

import com.didit.adapter.webapi.auth.dto.KakaoTokenResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class KakaoAuthAdapterTest {
    private val restTemplate = mock(RestTemplate::class.java)

    private val adapter =
        KakaoAuthAdapter(
            restTemplate = restTemplate,
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
        )

    @Test
    fun `카카오_idToken_조회_성공`() {
        val code = "auth-code"
        val redirectUri = "http://localhost:8080/auth/social/kakao"

        val kakaoResponse =
            KakaoTokenResponse(
                accessToken = "dummy-access-token",
                idToken = "id-token",
            )

        `when`(
            restTemplate.postForEntity(
                anyString(),
                any(),
                eq(KakaoTokenResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(kakaoResponse))

        val result = adapter.getIdToken(code, redirectUri)

        assertEquals("id-token", result)

        verify(restTemplate).postForEntity(
            anyString(),
            any(),
            eq(KakaoTokenResponse::class.java),
        )
    }

    @Test
    fun `카카오_idToken_없음_예외`() {
        val kakaoResponse =
            KakaoTokenResponse(
                accessToken = "dummy-access-token",
                idToken = null,
            )

        `when`(
            restTemplate.postForEntity(
                anyString(),
                any(),
                eq(KakaoTokenResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(kakaoResponse))

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                adapter.getIdToken("code", "redirect-uri")
            }

        assertEquals("id_token 없음 (scope=openid 확인)", ex.message)
    }

    @Test
    fun `카카오_응답_body_null_예외`() {
        `when`(
            restTemplate.postForEntity(
                anyString(),
                any(),
                eq(KakaoTokenResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(null))

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                adapter.getIdToken("code", "redirect-uri")
            }

        assertEquals("Kakao token 요청 실패", ex.message)
    }

    @Test
    fun `카카오_API_호출_실패_예외`() {
        `when`(
            restTemplate.postForEntity(
                anyString(),
                any(),
                eq(KakaoTokenResponse::class.java),
            ),
        ).thenThrow(RuntimeException("API error"))

        assertThrows(RuntimeException::class.java) {
            adapter.getIdToken("code", "redirect-uri")
        }
    }
}
