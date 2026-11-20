package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.AppleTokenExchange
import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.InvalidSocialCredentialTypeException
import com.didit.application.auth.exception.InvalidSocialRedirectUriException
import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.application.auth.required.AppleTokenClient
import com.didit.application.auth.required.OAuthClient
import com.didit.domain.auth.SocialCredentialType
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date

@Component
class AppleOAuthClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @param:Value("\${oauth.apple.jwks-url}") private val jwksUrl: String,
    @Value("\${oauth.apple.allowed-client-ids:}") allowedClientIdsConfig: String,
    @Value("\${oauth.apple.native-client-ids:}") nativeClientIdsConfig: String,
    @Value("\${oauth.apple.token-url}") private val tokenUrl: String = "https://appleid.apple.com/auth/token",
    @Value("\${oauth.apple.revoke-url:https://appleid.apple.com/auth/revoke}") private val revokeUrl: String =
        "https://appleid.apple.com/auth/revoke",
    @Value("\${oauth.apple.client-id:}") private val clientId: String = "",
    @Value("\${oauth.apple.team-id:}") private val teamId: String = "",
    @Value("\${oauth.apple.key-id:}") private val keyId: String = "",
    @Value("\${oauth.apple.private-key-pem-base64:}") private val privateKeyPemBase64: String = "",
    @Value("\${oauth.apple.allowed-redirect-uris:}") allowedRedirectUrisConfig: String = "",
) : OAuthClient,
    AppleTokenClient {
    private val allowedClientIds = parseAllowList(allowedClientIdsConfig)
    private val nativeClientIds = parseAllowList(nativeClientIdsConfig)
    private val allowedRedirectUris = parseAllowList(allowedRedirectUrisConfig)
    private val clientSecretPrivateKey: PrivateKey by lazy(::parseClientSecretPrivateKey)

    override fun getUserInfo(oauthToken: String): UserInfo = verifyIdToken(oauthToken, nativeClientIds)

    override fun exchangeNativeAuthorizationCode(
        idToken: String,
        authorizationCode: String,
    ): AppleTokenExchange {
        val originalIdentity = verifyAppleIdentity(idToken, nativeClientIds)
        val response = exchangeAuthorizationCode(authorizationCode, originalIdentity.audience, null)
        val exchangedIdentity = verifyAppleIdentity(response.idToken, setOf(originalIdentity.audience))
        if (exchangedIdentity.userInfo.providerId != originalIdentity.userInfo.providerId) {
            throw OAuthUserInfoFailedException()
        }
        val refreshToken = response.refreshToken?.takeIf(String::isNotBlank) ?: throw OAuthUserInfoFailedException()
        return AppleTokenExchange(originalIdentity.userInfo, originalIdentity.audience, refreshToken)
    }

    override fun exchangeWebAuthorizationCode(
        authorizationCode: String,
        redirectUri: String?,
        expectedNonce: String?,
    ): AppleTokenExchange {
        if (expectedNonce.isNullOrBlank()) throw OAuthUserInfoFailedException()
        if (clientId.isBlank() || clientId !in allowedClientIds) throw OAuthUserInfoFailedException()
        val validatedRedirectUri = redirectUri?.takeIf { it in allowedRedirectUris } ?: throw InvalidSocialRedirectUriException()
        val response = exchangeAuthorizationCode(authorizationCode, clientId, validatedRedirectUri)
        val identity = verifyAppleIdentity(response.idToken, setOf(clientId), expectedNonce)
        val refreshToken = response.refreshToken?.takeIf(String::isNotBlank) ?: throw OAuthUserInfoFailedException()
        return AppleTokenExchange(identity.userInfo, clientId, refreshToken)
    }

    override fun revoke(
        tokenClientId: String,
        refreshToken: String,
    ) {
        if (tokenClientId !in allowedClientIds + nativeClientIds || refreshToken.isBlank()) throw OAuthUserInfoFailedException()
        runCatching {
            val form =
                LinkedMultiValueMap<String, String>().apply {
                    add("client_id", tokenClientId)
                    add("client_secret", createClientSecret(tokenClientId))
                    add("token", refreshToken)
                    add("token_type_hint", "refresh_token")
                }
            restClient
                .post()
                .uri(revokeUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity()
        }.getOrElse { throw OAuthUserInfoFailedException() }
    }

    override fun getUserInfo(
        credentialType: SocialCredentialType,
        credential: String,
    ): UserInfo {
        if (credentialType != SocialCredentialType.ID_TOKEN) throw InvalidSocialCredentialTypeException()
        return getUserInfo(credential)
    }

    override fun getUserInfo(
        credentialType: SocialCredentialType,
        credential: String,
        redirectUri: String?,
        expectedNonce: String?,
    ): UserInfo =
        when (credentialType) {
            SocialCredentialType.ID_TOKEN -> getUserInfo(credential)
            SocialCredentialType.AUTHORIZATION_CODE -> {
                if (expectedNonce.isNullOrBlank()) throw OAuthUserInfoFailedException()
                verifyIdToken(exchangeAuthorizationCode(credential, redirectUri), setOf(clientId), expectedNonce)
            }
            else -> throw InvalidSocialCredentialTypeException()
        }

    private fun verifyIdToken(
        idToken: String,
        acceptedAudiences: Set<String>,
        expectedNonce: String? = null,
    ): UserInfo = verifyAppleIdentity(idToken, acceptedAudiences, expectedNonce).userInfo

    private fun verifyAppleIdentity(
        idToken: String,
        acceptedAudiences: Set<String>,
        expectedNonce: String? = null,
    ): VerifiedAppleIdentity {
        if (acceptedAudiences.isEmpty() || acceptedAudiences.any(String::isBlank)) throw OAuthUserInfoFailedException()

        val claims =
            runCatching {
                val applePublicKey = fetchApplePublicKey(idToken)
                Jwts
                    .parser()
                    .verifyWith(applePublicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .payload
            }.getOrElse { throw OAuthUserInfoFailedException() }

        if (claims.issuer != APPLE_ISSUER ||
            claims.audience?.any { it in acceptedAudiences } != true ||
            claims.expiration?.toInstant()?.isAfter(Instant.now()) != true ||
            claims.subject.isNullOrBlank() ||
            (expectedNonce != null && claims["nonce"] != expectedNonce)
        ) {
            throw OAuthUserInfoFailedException()
        }

        val audience = claims.audience?.singleOrNull { it in acceptedAudiences } ?: throw OAuthUserInfoFailedException()
        return VerifiedAppleIdentity(
            userInfo =
                UserInfo(
                    providerId = claims.subject,
                    email = claims["email"] as? String,
                ),
            audience = audience,
        )
    }

    private fun exchangeAuthorizationCode(
        code: String,
        redirectUri: String?,
    ): String {
        if (code.isBlank() || clientId.isBlank() || clientId !in allowedClientIds) throw OAuthUserInfoFailedException()
        val validatedRedirectUri = redirectUri?.takeIf { it in allowedRedirectUris } ?: throw InvalidSocialRedirectUriException()
        return exchangeAuthorizationCode(code, clientId, validatedRedirectUri).idToken.takeIf(String::isNotBlank)
            ?: throw OAuthUserInfoFailedException()
    }

    private fun exchangeAuthorizationCode(
        code: String,
        tokenClientId: String,
        redirectUri: String?,
    ): AppleTokenResponse {
        if (code.isBlank() || tokenClientId !in allowedClientIds + nativeClientIds) throw OAuthUserInfoFailedException()
        return runCatching {
            val form =
                LinkedMultiValueMap<String, String>().apply {
                    add("grant_type", "authorization_code")
                    add("client_id", tokenClientId)
                    add("client_secret", createClientSecret(tokenClientId))
                    redirectUri?.let { add("redirect_uri", it) }
                    add("code", code)
                }
            restClient
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body<AppleTokenResponse>()
        }.getOrElse { throw OAuthUserInfoFailedException() }
            ?: throw OAuthUserInfoFailedException()
    }

    private fun createClientSecret(tokenClientId: String): String {
        require(teamId.isNotBlank() && keyId.isNotBlank() && privateKeyPemBase64.isNotBlank())
        val now = Instant.now()
        return Jwts
            .builder()
            .header()
            .keyId(keyId)
            .and()
            .issuer(teamId)
            .audience()
            .add(APPLE_ISSUER)
            .and()
            .subject(tokenClientId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(300)))
            .signWith(clientSecretPrivateKey, Jwts.SIG.ES256)
            .compact()
    }

    private fun parseClientSecretPrivateKey(): PrivateKey {
        val pem = String(Base64.getDecoder().decode(privateKeyPemBase64), StandardCharsets.UTF_8)
        val encodedKey =
            pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .filterNot(Char::isWhitespace)
        return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey)))
    }

    private fun fetchApplePublicKey(idToken: String): RSAPublicKey {
        val segments = idToken.split(".")
        require(segments.size == 3)
        val idTokenHeader =
            objectMapper.readTree(String(Base64.getUrlDecoder().decode(segments[0]), StandardCharsets.UTF_8))
        require(idTokenHeader.path("alg").asText() == "RS256")
        val signingKeyId = idTokenHeader.path("kid").asText()
        require(signingKeyId.isNotBlank())

        val appleJwks =
            restClient
                .get()
                .uri(jwksUrl)
                .retrieve()
                .body<AppleJwks>()
                ?: throw OAuthUserInfoFailedException()
        val matchedKey =
            appleJwks.keys.find {
                it.kid == signingKeyId && it.kty == "RSA" && it.alg == "RS256" && it.use == "sig"
            } ?: throw OAuthUserInfoFailedException()

        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(matchedKey.n))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(matchedKey.e))
        return KeyFactory
            .getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(modulus, exponent)) as RSAPublicKey
    }

    private data class AppleTokenResponse(
        @param:JsonProperty("id_token") val idToken: String,
        @param:JsonProperty("refresh_token") val refreshToken: String? = null,
    )

    private data class VerifiedAppleIdentity(
        val userInfo: UserInfo,
        val audience: String,
    )

    private data class AppleJwks(
        val keys: List<AppleJwk>,
    )

    private data class AppleJwk(
        val kid: String,
        val kty: String,
        val alg: String,
        val use: String,
        val n: String,
        val e: String,
    )

    private companion object {
        const val APPLE_ISSUER = "https://appleid.apple.com"

        fun parseAllowList(config: String): Set<String> =
            config
                .split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
    }
}
