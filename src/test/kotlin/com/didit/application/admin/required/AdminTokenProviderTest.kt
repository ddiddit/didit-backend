package com.didit.application.admin.required

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
    fun `generateAccessToken`() {
        val adminId = UUID.randomUUID()
        whenever(adminTokenProvider.generateAccessToken(adminId)).thenReturn("access-token")

        val token = adminTokenProvider.generateAccessToken(adminId)

        verify(adminTokenProvider).generateAccessToken(adminId)
        assertThat(token).isEqualTo("access-token")
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
