package com.didit.application.auth.provided

import com.didit.application.auth.dto.TokenResponse
import com.didit.domain.auth.Provider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthTest {
    @Mock
    lateinit var auth: Auth

    @Test
    fun `login`() {
        val response = TokenResponse(accessToken = "access-token", refreshToken = "refresh-token", isNewUser = true)
        whenever(auth.login(Provider.KAKAO, "oauth-token")).thenReturn(response)

        val result = auth.login(Provider.KAKAO, "oauth-token")

        verify(auth).login(Provider.KAKAO, "oauth-token")
        assertThat(result.isNewUser).isTrue()
        assertThat(result.accessToken).isEqualTo("access-token")
    }

    @Test
    fun `logout`() {
        val userId = UUID.randomUUID()

        auth.logout(userId)

        verify(auth).logout(userId)
    }

    @Test
    fun `withdraw`() {
        val userId = UUID.randomUUID()

        auth.withdraw(userId)

        verify(auth).withdraw(userId)
    }

    @Test
    fun `refresh`() {
        val response = TokenResponse(accessToken = "new-access-token", refreshToken = "new-refresh-token")
        whenever(auth.refresh("refresh-token")).thenReturn(response)

        val result = auth.refresh("refresh-token")

        verify(auth).refresh("refresh-token")
        assertThat(result.accessToken).isEqualTo("new-access-token")
        assertThat(result.refreshToken).isEqualTo("new-refresh-token")
    }
}
