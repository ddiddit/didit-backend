package com.didit.application.auth.required

import com.didit.support.RefreshTokenFixture
import com.didit.support.RepositoryTestSupport
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
}
