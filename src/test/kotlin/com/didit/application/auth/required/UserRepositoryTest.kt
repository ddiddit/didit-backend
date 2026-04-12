package com.didit.application.auth.required

import com.didit.domain.auth.Provider
import com.didit.support.RepositoryTestSupport
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class UserRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `save`() {
        val user = UserFixture.create()

        val saved = userRepository.save(user)

        assertThat(saved.provider).isEqualTo(Provider.KAKAO)
        assertThat(saved.providerId).isEqualTo("kakao-0325")
    }

    @Test
    fun `findById`() {
        val user = userRepository.save(UserFixture.create())

        val found = userRepository.findById(user.id)

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(user.id)
    }

    @Test
    fun `findById - not found`() {
        val found = userRepository.findById(java.util.UUID.randomUUID())

        assertThat(found).isNull()
    }

    @Test
    fun `findByProviderAndProviderId`() {
        val user = userRepository.save(UserFixture.create())

        val found = userRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-0325")

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(user.id)
    }

    @Test
    fun `findByProviderAndProviderId - not found`() {
        val found = userRepository.findByProviderAndProviderId(Provider.KAKAO, "unknown")

        assertThat(found).isNull()
    }

    @Test
    fun `existsByNickname - exists`() {
        val user = userRepository.save(UserFixture.createOnboarded())

        val exists = userRepository.existsByNicknameAndDeletedAtIsNull(user.nickname!!)

        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByNickname - not exists`() {
        val exists = userRepository.existsByNicknameAndDeletedAtIsNull("없는닉네임")

        assertThat(exists).isFalse()
    }

    @Test
    fun `existsByNicknameAndIdNot - 다른 유저가 같은 닉네임 사용 중`() {
        val user = userRepository.save(UserFixture.createOnboarded())
        val otherUser = userRepository.save(UserFixture.createOnboarded(providerId = "kakao-9999"))

        val exists = userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(user.nickname!!, otherUser.id)

        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByNicknameAndIdNot - 본인 닉네임은 중복 아님`() {
        val user = userRepository.save(UserFixture.createOnboarded())

        val exists = userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(user.nickname!!, user.id)

        assertThat(exists).isFalse()
    }

    @Test
    fun `existsByNicknameAndIdNot - 닉네임 없음`() {
        val user = userRepository.save(UserFixture.createOnboarded())

        val exists = userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("없는닉네임", user.id)

        assertThat(exists).isFalse()
    }

    @Test
    fun `findAllWithdrawnBefore - 30일 지난 탈퇴 유저만 반환한다`() {
        val withdrawn = userRepository.save(UserFixture.create().apply { withdraw() })
        val active = userRepository.save(UserFixture.create(providerId = "kakao-9999"))

        val result = userRepository.findAllWithdrawnBefore(LocalDateTime.now().plusDays(1))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(withdrawn.id)
    }

    @Test
    fun `findAllWithdrawnBefore - cutoff 이후 탈퇴 유저는 제외된다`() {
        userRepository.save(UserFixture.create().apply { withdraw() })

        val result = userRepository.findAllWithdrawnBefore(LocalDateTime.now().minusDays(1))

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAllWithdrawnBefore - 탈퇴하지 않은 유저는 제외된다`() {
        userRepository.save(UserFixture.create())

        val result = userRepository.findAllWithdrawnBefore(LocalDateTime.now().plusDays(1))

        assertThat(result).isEmpty()
    }
}
