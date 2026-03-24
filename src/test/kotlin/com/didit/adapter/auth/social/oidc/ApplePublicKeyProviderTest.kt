package com.didit.adapter.auth.social.oidc

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ApplePublicKeyProviderTest {
    private val provider = ApplePublicKeyProvider()

    @Test
    fun `존재하지_않는_kid_예외_처리`() {
        assertThrows<IllegalArgumentException> {
            provider.getPublicKey("non-existent-kid")
        }
    }
}
