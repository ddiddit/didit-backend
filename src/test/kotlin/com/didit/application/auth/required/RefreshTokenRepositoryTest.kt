package com.didit.application.auth.required

import com.didit.domain.auth.RefreshToken
import com.didit.support.RefreshTokenFixture
import com.didit.support.RepositoryTestSupport
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class RefreshTokenRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Test
    fun `save`() {
        val user = userRepository.save(UserFixture.create())
        val token = RefreshTokenFixture.create(userId = user.id)

        val saved = refreshTokenRepository.save(token)

        assertThat(saved.userId).isEqualTo(user.id)
        assertThat(saved.token).isEqualTo("refresh-token")
    }

    @Test
    fun `findByToken`() {
        val user = userRepository.save(UserFixture.create())
        refreshTokenRepository.save(RefreshTokenFixture.create(userId = user.id))

        val found = refreshTokenRepository.findByToken("refresh-token")

        assertThat(found).isNotNull
        assertThat(found?.userId).isEqualTo(user.id)
    }

    @Test
    fun `findByToken - not found`() {
        val found = refreshTokenRepository.findByToken("unknown-token")

        assertThat(found).isNull()
    }

    @Test
    fun `deleteByUserId`() {
        val user = userRepository.save(UserFixture.create())
        refreshTokenRepository.save(RefreshTokenFixture.create(userId = user.id))

        refreshTokenRepository.deleteByUserId(user.id)

        val found = refreshTokenRepository.findByToken("refresh-token")
        assertThat(found).isNull()
    }

    @Test
    fun `deleteByUserId - not exists`() {
        refreshTokenRepository.deleteByUserId(UUID.randomUUID())
    }

    @Test
    fun `deleteAllExpiredBefore - 만료된 토큰만 삭제된다`() {
        val user1 = userRepository.save(UserFixture.create(providerId = "kakao-0001"))
        val user2 = userRepository.save(UserFixture.create(providerId = "kakao-0002"))

        refreshTokenRepository.save(
            RefreshToken.create(
                userId = user1.id,
                token = "expired-token",
                expiresAt = LocalDateTime.now().minusDays(1),
            ),
        )
        refreshTokenRepository.save(
            RefreshToken.create(
                userId = user2.id,
                token = "valid-token",
                expiresAt = LocalDateTime.now().plusDays(1),
            ),
        )

        val count = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now())

        assertThat(count).isEqualTo(1)
        assertThat(refreshTokenRepository.findByToken("expired-token")).isNull()
        assertThat(refreshTokenRepository.findByToken("valid-token")).isNotNull()
    }

    @Test
    fun `deleteAllExpiredBefore - 만료된 토큰이 없으면 0을 반환한다`() {
        val user = userRepository.save(UserFixture.create())

        refreshTokenRepository.save(
            RefreshToken.create(
                userId = user.id,
                token = "valid-token",
                expiresAt = LocalDateTime.now().plusDays(1),
            ),
        )

        val count = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now())

        assertThat(count).isEqualTo(0)
    }
}
