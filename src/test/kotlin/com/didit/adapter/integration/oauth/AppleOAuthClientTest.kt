package com.didit.adapter.integration.oauth

import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.domain.auth.SocialCredentialType
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64
import java.util.Date

class AppleOAuthClientTest {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val clientSecretKeyPair: KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    @Test
    fun `Apple ID 토큰의 서명과 발급자와 대상을 검증한다`() {
        val (client, server) = clientWithAppleKey()
        val result = client.getUserInfo(SocialCredentialType.ID_TOKEN, token(audience = "ios-bundle-id"))

        assertThat(result.providerId).isEqualTo("existing-apple-user")
        assertThat(result.email).isEqualTo("member@example.com")
        server.verify()
    }

    @Test
    fun `Apple이 아닌 발급자의 토큰은 서명이 유효해도 거절한다`() {
        val (client, server) = clientWithAppleKey()

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.ID_TOKEN, token(issuer = "https://attacker.example", audience = "ios-bundle-id"))
        }.isInstanceOf(OAuthUserInfoFailedException::class.java)
        server.verify()
    }

    @Test
    fun `허용되지 않은 대상의 토큰은 서명이 유효해도 거절한다`() {
        val (client, server) = clientWithAppleKey()

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.ID_TOKEN, token(audience = "other-app"))
        }.isInstanceOf(OAuthUserInfoFailedException::class.java)
        server.verify()
    }

    @Test
    fun `Apple 웹 인가 코드를 교환해 기존 사용자를 찾는다`() {
        val (client, server) = clientWithAppleKey(codeExchangeToken = token(nonce = "expected-nonce"))

        val result =
            client.getUserInfo(
                SocialCredentialType.AUTHORIZATION_CODE,
                "one-time-code",
                "https://app.didit.io.kr",
                "expected-nonce",
            )

        assertThat(result.providerId).isEqualTo("existing-apple-user")
        server.verify()
    }

    @Test
    fun `Apple 웹 인가 코드를 교환해 탈퇴용 refresh token을 반환한다`() {
        val (client, server) =
            clientWithAppleKey(
                codeExchangeToken = token(nonce = "expected-nonce"),
                codeExchangeRefreshToken = "apple-web-refresh-token",
            )

        val result =
            client.exchangeWebAuthorizationCode(
                "one-time-code",
                "https://app.didit.io.kr",
                "expected-nonce",
            )

        assertThat(result.userInfo.providerId).isEqualTo("existing-apple-user")
        assertThat(result.clientId).isEqualTo("web-client-id")
        assertThat(result.refreshToken).isEqualTo("apple-web-refresh-token")
        server.verify()
    }

    @Test
    fun `iOS Bundle ID 대상 Apple 토큰도 검증한다`() {
        val (client, server) = clientWithAppleKey()

        val result = client.getUserInfo(SocialCredentialType.ID_TOKEN, token(audience = "ios-bundle-id"))

        assertThat(result.providerId).isEqualTo("existing-apple-user")
        server.verify()
    }

    @Test
    fun `Apple client ID와 같은 iOS Bundle ID 대상 토큰도 검증한다`() {
        val (client, server) =
            clientWithAppleKey(
                allowedClientIdsConfig = "com.swyp.didit",
                nativeClientIdsConfig = "com.swyp.didit",
                clientId = "com.swyp.didit",
            )

        val result = client.getUserInfo(SocialCredentialType.ID_TOKEN, token(audience = "com.swyp.didit"))

        assertThat(result.providerId).isEqualTo("existing-apple-user")
        server.verify()
    }

    @Test
    fun `웹 Services ID 토큰을 코드 교환 없이 보내면 거절한다`() {
        val (client, server) = clientWithAppleKey()

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.ID_TOKEN, token(audience = "web-client-id"))
        }.isInstanceOf(OAuthUserInfoFailedException::class.java)
        server.verify()
    }

    @Test
    fun `웹 인가 코드 교환에서 iOS 대상 토큰은 거절한다`() {
        val (client, server) = clientWithAppleKey(codeExchangeToken = token(audience = "ios-bundle-id"))

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.AUTHORIZATION_CODE, "one-time-code", "https://app.didit.io.kr", "expected-nonce")
        }.isInstanceOf(OAuthUserInfoFailedException::class.java)
        server.verify()
    }

    @Test
    fun `웹 인가 코드 교환에 nonce가 없으면 거절한다`() {
        val (client, server) = clientWithAppleKey(codeExchangeToken = token())

        assertThatThrownBy {
            client.getUserInfo(SocialCredentialType.AUTHORIZATION_CODE, "one-time-code", "https://app.didit.io.kr", "expected-nonce")
        }.isInstanceOf(OAuthUserInfoFailedException::class.java)
        server.verify()
    }

    @Test
    fun `iOS 인가 코드를 Bundle ID로 교환해 탈퇴용 refresh token을 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        expectAppleKey(server)
        server
            .expect(requestTo(TOKEN_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("client_id=ios-bundle-id")))
            .andExpect(content().string(containsString("code=one-time-code")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("redirect_uri="))))
            .andRespond(
                withSuccess(
                    """{"id_token":"${token(audience = "ios-bundle-id")}","refresh_token":"apple-refresh-token"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
        expectAppleKey(server)
        val client = client(builder)

        val result =
            client.exchangeNativeAuthorizationCode(
                token(audience = "ios-bundle-id"),
                "one-time-code",
            )

        assertThat(result.clientId).isEqualTo("ios-bundle-id")
        assertThat(result.refreshToken).isEqualTo("apple-refresh-token")
        server.verify()
    }

    @Test
    fun `Apple refresh token을 발급받은 client ID로 해제한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server
            .expect(requestTo(REVOKE_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("client_id=ios-bundle-id")))
            .andExpect(content().string(containsString("token=apple-refresh-token")))
            .andExpect(content().string(containsString("token_type_hint=refresh_token")))
            .andRespond(withSuccess())
        val client = client(builder)

        client.revoke("ios-bundle-id", "apple-refresh-token")

        server.verify()
    }

    private fun clientWithAppleKey(
        codeExchangeToken: String? = null,
        codeExchangeRefreshToken: String? = null,
        allowedClientIdsConfig: String = "web-client-id",
        nativeClientIdsConfig: String = "ios-bundle-id",
        clientId: String = "web-client-id",
    ): Pair<AppleOAuthClient, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        if (codeExchangeToken != null) {
            server
                .expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_id=web-client-id")))
                .andExpect(content().string(containsString("code=one-time-code")))
                .andExpect(content().string(containsString("client_secret=")))
                .andRespond(
                    withSuccess(
                        """{"id_token":"$codeExchangeToken"${codeExchangeRefreshToken?.let { ",\"refresh_token\":\"$it\"" } ?: ""}}""",
                        MediaType.APPLICATION_JSON,
                    ),
                )
        }
        expectAppleKey(server)
        return client(
            builder = builder,
            allowedClientIdsConfig = allowedClientIdsConfig,
            nativeClientIdsConfig = nativeClientIdsConfig,
            clientId = clientId,
        ) to server
    }

    private fun client(
        builder: RestClient.Builder,
        allowedClientIdsConfig: String = "web-client-id",
        nativeClientIdsConfig: String = "ios-bundle-id",
        clientId: String = "web-client-id",
    ) = AppleOAuthClient(
        restClient = builder.build(),
        objectMapper = ObjectMapper(),
        jwksUrl = JWKS_URL,
        allowedClientIdsConfig = allowedClientIdsConfig,
        nativeClientIdsConfig = nativeClientIdsConfig,
        tokenUrl = TOKEN_URL,
        clientId = clientId,
        teamId = "TEAM123",
        keyId = "KEY123",
        privateKeyPemBase64 = privateKeyPemBase64(),
        allowedRedirectUrisConfig = "https://app.didit.io.kr",
        revokeUrl = REVOKE_URL,
    )

    private fun expectAppleKey(server: MockRestServiceServer) {
        val publicKey = keyPair.public as RSAPublicKey
        val jwks =
            """
            {"keys":[{"kid":"apple-test-key","kty":"RSA","alg":"RS256","use":"sig",
              "n":"${encode(publicKey.modulus)}","e":"${encode(publicKey.publicExponent)}"}]}
            """.trimIndent()
        server.expect(requestTo(JWKS_URL)).andRespond(withSuccess(jwks, MediaType.APPLICATION_JSON))
    }

    private fun token(
        issuer: String = "https://appleid.apple.com",
        audience: String = "web-client-id",
        nonce: String? = null,
    ): String =
        Jwts
            .builder()
            .header()
            .keyId("apple-test-key")
            .and()
            .issuer(issuer)
            .audience()
            .add(audience)
            .and()
            .subject("existing-apple-user")
            .claim("email", "member@example.com")
            .apply { nonce?.let { claim("nonce", it) } }
            .expiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(keyPair.private)
            .compact()

    private fun privateKeyPemBase64(): String {
        val encodedKey = Base64.getEncoder().encodeToString(clientSecretKeyPair.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$encodedKey\n-----END PRIVATE KEY-----"
        return Base64.getEncoder().encodeToString(pem.toByteArray(StandardCharsets.UTF_8))
    }

    private fun encode(number: BigInteger): String {
        val bytes = number.toByteArray()
        val positiveBytes = if (bytes.first() == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        return Base64.getUrlEncoder().withoutPadding().encodeToString(positiveBytes)
    }

    companion object {
        private const val JWKS_URL = "https://appleid.apple.com/auth/keys"
        private const val TOKEN_URL = "https://appleid.apple.com/auth/token"
        private const val REVOKE_URL = "https://appleid.apple.com/auth/revoke"
    }
}
