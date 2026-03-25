package com.didit.application.auth.required

import com.didit.support.RepositoryTestSupport
import com.didit.support.UserConsentFixture
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserConsentRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository

    @Test
    fun `save - with marketing agreed`() {
        val user = userRepository.save(UserFixture.create())
        val consent = UserConsentFixture.create(userId = user.id, marketingAgreed = true)

        val saved = userConsentRepository.save(consent)

        assertThat(saved.userId).isEqualTo(user.id)
        assertThat(saved.marketingAgreed).isTrue()
        assertThat(saved.marketingAgreedAt).isNotNull()
    }

    @Test
    fun `save - without marketing agreed`() {
        val user = userRepository.save(UserFixture.create())
        val consent = UserConsentFixture.create(userId = user.id, marketingAgreed = false)

        val saved = userConsentRepository.save(consent)

        assertThat(saved.marketingAgreed).isFalse()
        assertThat(saved.marketingAgreedAt).isNull()
    }
}
