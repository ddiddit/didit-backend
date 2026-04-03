package com.didit.domain.auth

import com.didit.support.UserConsentFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class UserConsentTest {
    @Test
    fun `create - with marketing agreed`() {
        val consent =
            UserConsent.create(
                userId = UUID.randomUUID(),
                marketingAgreed = true,
            )

        assertThat(consent.marketingAgreed).isTrue()
        assertThat(consent.marketingAgreedAt).isNotNull()
        assertThat(consent.marketingRevokedAt).isNull()
        assertThat(consent.serviceTermsAgreedAt).isNotNull()
        assertThat(consent.privacyAgreedAt).isNotNull()
    }

    @Test
    fun `create - without marketing agreed`() {
        val consent = UserConsentFixture.create(marketingAgreed = false)

        assertThat(consent.marketingAgreed).isFalse()
        assertThat(consent.marketingAgreedAt).isNull()
    }

    @Test
    fun `updateMarketing - agree`() {
        val consent = UserConsentFixture.create(marketingAgreed = false)
        val now = LocalDateTime.now()

        consent.updateMarketing(agreed = true, now = now)

        assertThat(consent.marketingAgreed).isTrue()
        assertThat(consent.marketingAgreedAt).isEqualTo(now)
        assertThat(consent.marketingRevokedAt).isNull()
    }

    @Test
    fun `updateMarketing - revoke`() {
        val consent = UserConsentFixture.create(marketingAgreed = true)
        val now = LocalDateTime.now()

        consent.updateMarketing(agreed = false, now = now)

        assertThat(consent.marketingAgreed).isFalse()
        assertThat(consent.marketingRevokedAt).isEqualTo(now)
    }
}
