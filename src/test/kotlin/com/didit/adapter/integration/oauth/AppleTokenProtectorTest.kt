package com.didit.adapter.integration.oauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AppleTokenProtectorTest {
    private val protector = AesAppleTokenProtector("test-key-material")

    @Test
    fun `Apple refresh token을 평문이 드러나지 않게 암호화하고 복호화한다`() {
        val protected = protector.protect("apple-refresh-token")

        assertThat(protected).doesNotContain("apple-refresh-token")
        assertThat(protector.unprotect(protected)).isEqualTo("apple-refresh-token")
    }

    @Test
    fun `같은 Apple refresh token도 매번 다른 암호문으로 저장한다`() {
        assertThat(protector.protect("apple-refresh-token"))
            .isNotEqualTo(protector.protect("apple-refresh-token"))
    }
}
