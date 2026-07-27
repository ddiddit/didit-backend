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
    fun `findAllWithdrawnAndNotAnonymizedBefore - 익명화되지 않은 탈퇴 유저만 반환한다`() {
        val withdrawn = userRepository.save(UserFixture.create().apply { withdraw() })

        userRepository.save(UserFixture.create(providerId = "kakao-9999"))

        val anonymized =
            userRepository.save(
                UserFixture.create(providerId = "kakao-1111").apply {
                    withdraw()
                    anonymize()
                },
            )

        val result = userRepository.findAllWithdrawnAndNotAnonymizedBefore(LocalDateTime.now().plusDays(1))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(withdrawn.id)
        assertThat(result).doesNotContain(anonymized)
    }

    @Test
    fun `findAllWithdrawnAndNotAnonymizedBefore - cutoff 이후 탈퇴 유저는 제외된다`() {
        userRepository.save(UserFixture.create().apply { withdraw() })

        val result = userRepository.findAllWithdrawnAndNotAnonymizedBefore(LocalDateTime.now().minusDays(1))

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAllWithdrawnAndAnonymizedBefore - 익명화된 탈퇴 유저만 반환한다`() {
        val anonymized =
            userRepository.save(
                UserFixture.create().apply {
                    withdraw()
                    anonymize()
                },
            )

        userRepository.save(UserFixture.create(providerId = "kakao-9999").apply { withdraw() })

        val result = userRepository.findAllWithdrawnAndAnonymizedBefore(LocalDateTime.now().plusDays(1))

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(anonymized.id)
    }

    @Test
    fun `findAllWithdrawnAndAnonymizedBefore - 탈퇴하지 않은 유저는 제외된다`() {
        userRepository.save(UserFixture.create())

        val result = userRepository.findAllWithdrawnAndAnonymizedBefore(LocalDateTime.now().plusDays(1))

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAllMarketingAgreedWithEmail - returns only active marketing agreed users with email`() {
        val marketingAgreed =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-1",
                    email = "agreed@test.com",
                    marketingAgreed = true,
                ),
            )
        userRepository.save(
            UserFixture.createOnboardedWithConsent(
                providerId = "kakao-2",
                email = "disagreed@test.com",
                marketingAgreed = false,
            ),
        )
        userRepository.save(
            UserFixture.createOnboardedWithConsent(
                providerId = "kakao-3",
                email = null,
                marketingAgreed = true,
            ),
        )
        userRepository.save(
            UserFixture
                .createOnboardedWithConsent(
                    providerId = "kakao-4",
                    email = "deleted@test.com",
                    marketingAgreed = true,
                ).apply { withdraw() },
        )
        userRepository.save(UserFixture.create(providerId = "kakao-5", email = "no-consent@test.com"))

        val result = userRepository.findAllMarketingAgreedWithEmail()

        assertThat(result).extracting("id").containsExactly(marketingAgreed.id)
    }

    @Test
    fun `findAllMarketingAgreedWithEmailByIdIn - returns only selected active marketing agreed users with email`() {
        val selected =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-1",
                    email = "agreed@test.com",
                    marketingAgreed = true,
                ),
            )
        val disagreed =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-2",
                    email = "disagreed@test.com",
                    marketingAgreed = false,
                ),
            )
        val noEmail =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-3",
                    email = null,
                    marketingAgreed = true,
                ),
            )
        val deleted =
            userRepository.save(
                UserFixture
                    .createOnboardedWithConsent(
                        providerId = "kakao-4",
                        email = "deleted@test.com",
                        marketingAgreed = true,
                    ).apply { withdraw() },
            )
        val noConsent = userRepository.save(UserFixture.create(providerId = "kakao-5", email = "no-consent@test.com"))

        val result =
            userRepository.findAllMarketingAgreedWithEmailByIdIn(
                listOf(selected.id, disagreed.id, noEmail.id, deleted.id, noConsent.id),
            )

        assertThat(result).extracting("id").containsExactly(selected.id)
    }

    @Test
    fun `findAllMarketingAgreed - returns active marketing agreed users regardless of email`() {
        val withEmail =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-push-1",
                    email = "agreed@test.com",
                    marketingAgreed = true,
                ),
            )
        val withoutEmail =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-push-2",
                    email = null,
                    marketingAgreed = true,
                ),
            )
        userRepository.save(
            UserFixture.createOnboardedWithConsent(
                providerId = "kakao-push-3",
                marketingAgreed = false,
            ),
        )
        userRepository.save(
            UserFixture
                .createOnboardedWithConsent(
                    providerId = "kakao-push-4",
                    marketingAgreed = true,
                ).apply { withdraw() },
        )
        userRepository.save(UserFixture.create(providerId = "kakao-push-5"))

        val result = userRepository.findAllMarketingAgreed()

        assertThat(result).extracting("id").containsExactlyInAnyOrder(withEmail.id, withoutEmail.id)
    }

    @Test
    fun `findAllMarketingAgreedByIdIn - returns only selected active marketing agreed users`() {
        val selected =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-push-1",
                    marketingAgreed = true,
                ),
            )
        val notSelected =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-push-2",
                    marketingAgreed = true,
                ),
            )
        val disagreed =
            userRepository.save(
                UserFixture.createOnboardedWithConsent(
                    providerId = "kakao-push-3",
                    marketingAgreed = false,
                ),
            )
        val deleted =
            userRepository.save(
                UserFixture
                    .createOnboardedWithConsent(
                        providerId = "kakao-push-4",
                        marketingAgreed = true,
                    ).apply { withdraw() },
            )

        val result =
            userRepository.findAllMarketingAgreedByIdIn(
                listOf(selected.id, disagreed.id, deleted.id),
            )

        assertThat(result).extracting("id").containsExactly(selected.id)
        assertThat(result).extracting("id").doesNotContain(notSelected.id)
    }
}
