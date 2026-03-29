package com.didit.application.auth.required

import com.didit.application.auth.dto.UserInfo
import com.didit.domain.auth.Provider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OAuthClientFactoryTest {
    private val kakaoClient =
        object : OAuthClient {
            override fun getUserInfo(oauthToken: String) = UserInfo(providerId = "kakao-123", email = "test@kakao.com")
        }

    private val googleClient =
        object : OAuthClient {
            override fun getUserInfo(oauthToken: String) = UserInfo(providerId = "google-123", email = "test@gmail.com")
        }

    private val appleClient =
        object : OAuthClient {
            override fun getUserInfo(oauthToken: String) = UserInfo(providerId = "apple-123", email = null)
        }

    private val factory =
        OAuthClientFactory(
            mapOf(
                Provider.KAKAO to kakaoClient,
                Provider.GOOGLE to googleClient,
                Provider.APPLE to appleClient,
            ),
        )

    @Test
    fun `getClient - kakao`() {
        val client = factory.getClient(Provider.KAKAO)
        val userInfo = client.getUserInfo("token")

        assertThat(userInfo.providerId).isEqualTo("kakao-123")
        assertThat(userInfo.email).isEqualTo("test@kakao.com")
    }

    @Test
    fun `getClient - google`() {
        val client = factory.getClient(Provider.GOOGLE)
        val userInfo = client.getUserInfo("token")

        assertThat(userInfo.providerId).isEqualTo("google-123")
        assertThat(userInfo.email).isEqualTo("test@gmail.com")
    }

    @Test
    fun `getClient - apple`() {
        val client = factory.getClient(Provider.APPLE)
        val userInfo = client.getUserInfo("token")

        assertThat(userInfo.providerId).isEqualTo("apple-123")
        assertThat(userInfo.email).isNull()
    }

    @Test
    fun `getClient - unsupported provider throws exception`() {
        val emptyFactory = OAuthClientFactory(emptyMap())

        assertThatThrownBy { emptyFactory.getClient(Provider.KAKAO) }
            .isInstanceOf(UnsupportedOAuthProviderException::class.java)
    }
}
