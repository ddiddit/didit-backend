package com.didit.application.auth.provided

import com.didit.application.auth.dto.RefreshResponse
import com.didit.application.auth.dto.TokenResponse
import com.didit.domain.auth.Provider
import com.didit.domain.auth.WithdrawalReason
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
        val response =
            TokenResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                isNewUser = true,
                isOnboardingCompleted = false,
            )
        whenever(auth.login(Provider.KAKAO, "oauth-token")).thenReturn(response)

        val result = auth.login(Provider.KAKAO, "oauth-token")

        verify(auth).login(Provider.KAKAO, "oauth-token")
        assertThat(result.isNewUser).isTrue()
        assertThat(result.isOnboardingCompleted).isFalse()
        assertThat(result.accessToken).isEqualTo("access-token")
    }

    @Test
    fun `logout`() {
        val userId = UUID.randomUUID()

        auth.logout(userId)

        verify(auth).logout(userId)
    }

    @Test
    fun `withdraw - 탈퇴 사유와 함께 탈퇴한다`() {
        val userId = UUID.randomUUID()

        auth.withdraw(userId, WithdrawalReason.NO_LONGER_NEEDED)

        verify(auth).withdraw(userId, WithdrawalReason.NO_LONGER_NEEDED)
    }

    @Test
    fun `withdraw - 기타 사유로 탈퇴한다`() {
        val userId = UUID.randomUUID()

        auth.withdraw(userId, WithdrawalReason.OTHER, "개인적인 사유입니다.")

        verify(auth).withdraw(userId, WithdrawalReason.OTHER, "개인적인 사유입니다.")
    }

    @Test
    fun `refresh`() {
        val response =
            RefreshResponse(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
            )
        whenever(auth.refresh("refresh-token")).thenReturn(response)

        val result = auth.refresh("refresh-token")

        verify(auth).refresh("refresh-token")
        assertThat(result.accessToken).isEqualTo("new-access-token")
        assertThat(result.refreshToken).isEqualTo("new-refresh-token")
    }
}
