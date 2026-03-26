package com.didit.application.admin.required

import com.didit.domain.admin.AdminRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminTokenProviderTest {
    @Mock
    lateinit var adminTokenProvider: AdminTokenProvider

    @Test
    fun `generateAccessToken - ADMIN`() {
        val adminId = UUID.randomUUID()
        whenever(adminTokenProvider.generateAccessToken(adminId, AdminRole.ADMIN)).thenReturn("access-token")

        val token = adminTokenProvider.generateAccessToken(adminId, AdminRole.ADMIN)

        verify(adminTokenProvider).generateAccessToken(adminId, AdminRole.ADMIN)
        assertThat(token).isEqualTo("access-token")
    }

    @Test
    fun `generateAccessToken - SUPER_ADMIN`() {
        val adminId = UUID.randomUUID()
        whenever(adminTokenProvider.generateAccessToken(adminId, AdminRole.SUPER_ADMIN)).thenReturn("super-access-token")

        val token = adminTokenProvider.generateAccessToken(adminId, AdminRole.SUPER_ADMIN)

        verify(adminTokenProvider).generateAccessToken(adminId, AdminRole.SUPER_ADMIN)
        assertThat(token).isEqualTo("super-access-token")
    }

    @Test
    fun `generateRefreshToken`() {
        whenever(adminTokenProvider.generateRefreshToken()).thenReturn("refresh-token")

        val token = adminTokenProvider.generateRefreshToken()

        verify(adminTokenProvider).generateRefreshToken()
        assertThat(token).isEqualTo("refresh-token")
    }

    @Test
    fun `getRefreshTokenExpiresAt`() {
        val expiresAt = LocalDateTime.now().plusDays(14)
        whenever(adminTokenProvider.getRefreshTokenExpiresAt()).thenReturn(expiresAt)

        val result = adminTokenProvider.getRefreshTokenExpiresAt()

        verify(adminTokenProvider).getRefreshTokenExpiresAt()
        assertThat(result).isEqualTo(expiresAt)
    }
}
