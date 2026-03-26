package com.didit.application.admin.provided

import com.didit.application.admin.dto.AdminRefreshResponse
import com.didit.application.admin.dto.AdminTokenResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminAuthTest {
    @Mock
    lateinit var adminAuth: AdminAuth

    @Test
    fun `login`() {
        val response =
            AdminTokenResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
            )
        whenever(adminAuth.login("admin@didit.com", "password")).thenReturn(response)

        val result = adminAuth.login("admin@didit.com", "password")

        verify(adminAuth).login("admin@didit.com", "password")
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
    }

    @Test
    fun `logout`() {
        val adminId = UUID.randomUUID()

        adminAuth.logout(adminId)

        verify(adminAuth).logout(adminId)
    }

    @Test
    fun `refresh`() {
        val response =
            AdminRefreshResponse(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
            )
        whenever(adminAuth.refresh("refresh-token")).thenReturn(response)

        val result = adminAuth.refresh("refresh-token")

        verify(adminAuth).refresh("refresh-token")
        assertThat(result.accessToken).isEqualTo("new-access-token")
        assertThat(result.refreshToken).isEqualTo("new-refresh-token")
    }
}
