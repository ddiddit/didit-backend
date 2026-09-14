package com.didit.adapter.integration.oauth

import com.didit.application.auth.exception.InvalidSocialRedirectUriException
import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.domain.auth.SocialCredentialType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class KakaoOAuthClientTest {
    @Test
    fun `인가 코드는 서버에서 교환하고 토큰의 Kakao 앱 ID까지 검증한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = createClient(builder, appId = 12345L)
        expectTokenExchange(server, LOCAL_REDIRECT_URI)
        server
            .expect(once(), requestTo(TOKEN_INFO_URL))
            .andExpect(header("Authorization", "Bearer kakao-access-token"))
            .andRespond(withSuccess("""{"app_id":12345}""", MediaType.APPLICATION_JSON))
        server
            .expect(once(), requestTo(USER_INFO_URL))
            .andExpect(header("Authorization", "Bearer kakao-access-token"))
            .andRespond(
                withSuccess(
                    """{"id":98765,"kakao_account":{"email":"member@example.com"}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.getUserInfo(SocialCredentialType.AUTHORIZATION_CODE, "authorization-code", LOCAL_REDIRECT_URI)

        assertThat(result.providerId).isEqualTo("98765")
        assertThat(result.email).isEqualTo("member@example.com")
        server.verify()
    }

    @Test
    fun `다른 Kakao 앱에서 발급된 액세스 토큰은 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = createClient(builder, appId = 12345L)
        expectTokenExchange(server, LOCAL_REDIRECT_URI)
        server
            .expect(once(), requestTo(TOKEN_INFO_URL))
            .andRespond(withSuccess("""{"app_id":99999}""", MediaType.APPLICATION_JSON))

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.AUTHORIZATION_CODE, "authorization-code", LOCAL_REDIRECT_URI)
        }.isInstanceOf(OAuthUserInfoFailedException::class.java)
    }

    @Test
    fun `허용되지 않은 callback URI는 Kakao 호출 전에 거절한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = createClient(builder, appId = 12345L)

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.AUTHORIZATION_CODE, "authorization-code", "https://evil.example/callback")
        }.isInstanceOf(InvalidSocialRedirectUriException::class.java)

        server.verify()
    }

    private fun createClient(
        builder: RestClient.Builder,
        appId: Long,
    ) = KakaoOAuthClient(
        restClient = builder.build(),
        userInfoUrl = USER_INFO_URL,
        tokenUrl = TOKEN_URL,
        tokenInfoUrl = TOKEN_INFO_URL,
        restApiKey = "rest-api-key",
        clientSecret = "client-secret",
        allowedRedirectUrisConfig = "$LOCAL_REDIRECT_URI,$DEV_REDIRECT_URI",
        appId = appId,
    )

    private fun expectTokenExchange(
        server: MockRestServiceServer,
        redirectUri: String,
    ) {
        server
            .expect(once(), requestTo(TOKEN_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.containsString("redirect_uri=${java.net.URLEncoder.encode(redirectUri, Charsets.UTF_8)}"),
                ),
            ).andRespond(
                withSuccess(
                    """{"access_token":"kakao-access-token"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
    }

    companion object {
        private const val TOKEN_URL = "https://kauth.kakao.com/oauth/token"
        private const val TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info"
        private const val USER_INFO_URL = "https://kapi.kakao.com/v2/user/me"
        private const val LOCAL_REDIRECT_URI = "http://localhost:3000/auth/kakao/callback"
        private const val DEV_REDIRECT_URI = "https://dev-app.didit.io.kr/auth/kakao/callback"
    }
}
