package com.didit.domain.admin

import com.didit.support.AdminRefreshTokenFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AdminRefreshTokenTest {
    @Test
    fun `create`() {
        val adminId = UUID.randomUUID()
        val expiresAt = LocalDateTime.now().plusDays(14)
        val token =
            AdminRefreshToken.create(
                adminId = adminId,
                token = "admin-refresh-token",
                expiresAt = expiresAt,
            )

        assertThat(token.adminId).isEqualTo(adminId)
        assertThat(token.token).isEqualTo("admin-refresh-token")
        assertThat(token.expiresAt).isEqualTo(expiresAt)
    }

    @Test
    fun `isExpired - not expired`() {
        val token = AdminRefreshTokenFixture.create(expiresAt = LocalDateTime.now().plusDays(14))

        assertThat(token.isExpired()).isFalse()
    }

    @Test
    fun `isExpired - expired`() {
        val token = AdminRefreshTokenFixture.create(expiresAt = LocalDateTime.now().minusDays(1))

        assertThat(token.isExpired()).isTrue()
    }

    @Test
    fun `rotate`() {
        val token = AdminRefreshTokenFixture.create()
        val newExpiresAt = LocalDateTime.now().plusDays(14)

        token.rotate("new-admin-token", newExpiresAt)

        assertThat(token.token).isEqualTo("new-admin-token")
        assertThat(token.expiresAt).isEqualTo(newExpiresAt)
    }
}
