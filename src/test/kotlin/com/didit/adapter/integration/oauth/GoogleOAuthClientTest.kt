package com.didit.adapter.integration.oauth

import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.domain.auth.SocialCredentialType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant

class GoogleOAuthClientTest {
    @Test
    fun `Google ID 토큰의 발급자 대상 만료시간을 검증한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = GoogleOAuthClient(builder.build(), TOKEN_INFO_URL, "web-client-id,ios-client-id")
        server
            .expect(requestTo("$TOKEN_INFO_URL?id_token=id-token"))
            .andRespond(
                withSuccess(
                    tokenInfoJson(audience = "web-client-id"),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.getUserInfo(SocialCredentialType.ID_TOKEN, "id-token")

        assertThat(result.providerId).isEqualTo("google-user-id")
        assertThat(result.email).isEqualTo("member@example.com")
        server.verify()
    }

    @Test
    fun `허용되지 않은 Google Client ID 토큰은 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = GoogleOAuthClient(builder.build(), TOKEN_INFO_URL, "web-client-id")
        server
            .expect(requestTo("$TOKEN_INFO_URL?id_token=id-token"))
            .andRespond(withSuccess(tokenInfoJson(audience = "attacker-client-id"), MediaType.APPLICATION_JSON))

        assertThatThrownBy { client.getUserInfo(SocialCredentialType.ID_TOKEN, "id-token") }
            .isInstanceOf(OAuthUserInfoFailedException::class.java)
    }

    private fun tokenInfoJson(audience: String) =
        """
        {
          "sub": "google-user-id",
          "email": "member@example.com",
          "aud": "$audience",
          "iss": "https://accounts.google.com",
          "exp": "${Instant.now().plusSeconds(300).epochSecond}"
        }
        """.trimIndent()

    companion object {
        private const val TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo"
    }
}
