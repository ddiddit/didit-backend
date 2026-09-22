package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.InvalidSocialCredentialTypeException
import com.didit.application.auth.exception.InvalidSocialRedirectUriException
import com.didit.application.auth.exception.OAuthUserInfoFailedException
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
    @Value("\${oauth.apple.token-url}") private val tokenUrl: String = "https://appleid.apple.com/auth/token",
    @Value("\${oauth.apple.client-id:}") private val clientId: String = "",
    @Value("\${oauth.apple.team-id:}") private val teamId: String = "",
    @Value("\${oauth.apple.key-id:}") private val keyId: String = "",
    @Value("\${oauth.apple.private-key-pem-base64:}") private val privateKeyPemBase64: String = "",
    @Value("\${oauth.apple.allowed-redirect-uris:}") allowedRedirectUrisConfig: String = "",
) : OAuthClient {
    private val allowedClientIds =
        allowedClientIdsConfig
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
    private val allowedRedirectUris =
        allowedRedirectUrisConfig
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()

    override fun getUserInfo(oauthToken: String): UserInfo = verifyIdToken(oauthToken, allowedClientIds - clientId)

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
    ): UserInfo {
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

        return UserInfo(
            providerId = claims.subject,
            email = claims["email"] as? String,
        )
    }

    private fun exchangeAuthorizationCode(
        code: String,
        redirectUri: String?,
    ): String {
        if (code.isBlank() || clientId.isBlank() || clientId !in allowedClientIds) throw OAuthUserInfoFailedException()
        val validatedRedirectUri = redirectUri?.takeIf { it in allowedRedirectUris } ?: throw InvalidSocialRedirectUriException()

        return runCatching {
            val form =
                LinkedMultiValueMap<String, String>().apply {
                    add("grant_type", "authorization_code")
                    add("client_id", clientId)
                    add("client_secret", createClientSecret())
                    add("redirect_uri", validatedRedirectUri)
                    add("code", code)
                }
            restClient
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body<AppleTokenResponse>()
                ?.idToken
                ?.takeIf(String::isNotBlank)
        }.getOrElse { throw OAuthUserInfoFailedException() }
            ?: throw OAuthUserInfoFailedException()
    }

    private fun createClientSecret(): String {
        require(teamId.isNotBlank() && keyId.isNotBlank() && privateKeyPemBase64.isNotBlank())
        val pem = String(Base64.getDecoder().decode(privateKeyPemBase64), StandardCharsets.UTF_8)
        val encodedKey =
            pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .filterNot(Char::isWhitespace)
        val privateKey = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey)))
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
            .subject(clientId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(300)))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact()
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
    }
}
