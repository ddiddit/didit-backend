package com.didit.adapter.auth.social

import com.didit.adapter.auth.social.oidc.AppleOidcVerifier
import com.didit.adapter.auth.social.oidc.GoogleOidcVerifier
import com.didit.adapter.auth.social.oidc.dto.AppleIdTokenPayload
import com.didit.application.auth.exception.InvalidIdTokenException
import com.didit.domain.auth.enums.SocialProvider
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SocialAuthAdapterTest {
    private val googleVerifier = mock(GoogleOidcVerifier::class.java)
    private val appleVerifier = mock(AppleOidcVerifier::class.java)
    private val adapter = SocialAuthAdapter(googleVerifier, appleVerifier)

    @Test
    fun `구글_소셜_로그인_토큰_검증_성공`() {
        val idToken = "googleToken"

        val googlePayload =
            mock(GoogleIdToken.Payload::class.java).apply {
                `when`(subject).thenReturn("google123")
                `when`(email).thenReturn("user@gmail.com")
            }

        `when`(googleVerifier.verify(idToken)).thenReturn(googlePayload)

        val result = adapter.verifyIdToken(SocialProvider.GOOGLE, idToken)

        assertEquals(SocialProvider.GOOGLE, result.provider)
        assertEquals("google123", result.socialId)
        assertEquals("user@gmail.com", result.email)

        verify(googleVerifier).verify(idToken)
    }

    @Test
    fun `구글_소셜_로그인_토큰_검증_실패`() {
        val idToken = "invalidGoogleToken"
        `when`(googleVerifier.verify(idToken)).thenThrow(RuntimeException("Invalid token"))

        assertThrows<InvalidIdTokenException> {
            adapter.verifyIdToken(SocialProvider.GOOGLE, idToken)
        }

        verify(googleVerifier).verify(idToken)
    }

    @Test
    fun `애플_소셜_로그인_토큰_검증_성공`() {
        val idToken = "appleToken"

        val applePayload =
            mock(AppleIdTokenPayload::class.java).apply {
                `when`(subject).thenReturn("apple123")
                `when`(email).thenReturn("user@apple.com")
            }

        `when`(appleVerifier.verify(idToken)).thenReturn(applePayload)

        val result = adapter.verifyIdToken(SocialProvider.APPLE, idToken)

        assertEquals(SocialProvider.APPLE, result.provider)
        assertEquals("apple123", result.socialId)
        assertEquals("user@apple.com", result.email)

        verify(appleVerifier).verify(idToken)
    }

    @Test
    fun `애플_소셜_로그인_토큰_검증_실패`() {
        val idToken = "invalidAppleToken"
        `when`(appleVerifier.verify(idToken)).thenThrow(RuntimeException("Invalid token"))

        assertThrows<InvalidIdTokenException> {
            adapter.verifyIdToken(SocialProvider.APPLE, idToken)
        }

        verify(appleVerifier).verify(idToken)
    }
}
