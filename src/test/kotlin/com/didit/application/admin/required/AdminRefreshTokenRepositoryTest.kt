package com.didit.application.admin.required

import com.didit.support.AdminRefreshTokenFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminRefreshTokenRepositoryTest {
    @Mock
    lateinit var adminRefreshTokenRepository: AdminRefreshTokenRepository

    @Test
    fun `save`() {
        val token = AdminRefreshTokenFixture.create()
        whenever(adminRefreshTokenRepository.save(token)).thenReturn(token)

        val saved = adminRefreshTokenRepository.save(token)

        verify(adminRefreshTokenRepository).save(token)
        assertThat(saved.token).isEqualTo(token.token)
    }

    @Test
    fun `findByToken`() {
        val token = AdminRefreshTokenFixture.create()
        whenever(adminRefreshTokenRepository.findByToken("admin-refresh-token")).thenReturn(token)

        val found = adminRefreshTokenRepository.findByToken("admin-refresh-token")

        verify(adminRefreshTokenRepository).findByToken("admin-refresh-token")
        assertThat(found?.token).isEqualTo("admin-refresh-token")
    }

    @Test
    fun `findByToken - not found`() {
        whenever(adminRefreshTokenRepository.findByToken("unknown-token")).thenReturn(null)

        val found = adminRefreshTokenRepository.findByToken("unknown-token")

        assertThat(found).isNull()
    }

    @Test
    fun `deleteByAdminId`() {
        val adminId = UUID.randomUUID()

        adminRefreshTokenRepository.deleteByAdminId(adminId)

        verify(adminRefreshTokenRepository).deleteByAdminId(adminId)
    }

    @Test
    fun `deleteByAdminId - not exists`() {
        adminRefreshTokenRepository.deleteByAdminId(UUID.randomUUID())
    }
}
