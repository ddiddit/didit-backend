package com.didit.domain.auth

import com.didit.support.RefreshTokenFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class RefreshTokenTest {
    @Test
    fun `create`() {
        val userId = UUID.randomUUID()
        val expiresAt = LocalDateTime.now().plusDays(14)
        val token =
            RefreshToken.create(
                userId = userId,
                token = "refresh-token",
                expiresAt = expiresAt,
            )

        assertThat(token.userId).isEqualTo(userId)
        assertThat(token.token).isEqualTo("refresh-token")
        assertThat(token.expiresAt).isEqualTo(expiresAt)
    }

    @Test
    fun `isExpired - not expired`() {
        val token = RefreshTokenFixture.create(expiresAt = LocalDateTime.now().plusDays(14))

        assertThat(token.isExpired()).isFalse()
    }

    @Test
    fun `isExpired - expired`() {
        val token = RefreshTokenFixture.create(expiresAt = LocalDateTime.now().minusDays(1))

        assertThat(token.isExpired()).isTrue()
    }

    @Test
    fun `rotate`() {
        val token = RefreshTokenFixture.create()
        val newExpiresAt = LocalDateTime.now().plusDays(14)

        token.rotate("new-token", newExpiresAt)

        assertThat(token.token).isEqualTo("new-token")
        assertThat(token.expiresAt).isEqualTo(newExpiresAt)
    }
}
