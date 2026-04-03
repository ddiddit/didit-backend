package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.application.auth.required.OAuthClient
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class AppleOAuthClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @param:Value("\${oauth.apple.jwks-url}") private val jwksUrl: String,
) : OAuthClient {
    override fun getUserInfo(oauthToken: String): UserInfo {
        val applePublicKey = fetchApplePublicKey(oauthToken)

        val claims =
            runCatching {
                Jwts
                    .parser()
                    .verifyWith(applePublicKey)
                    .build()
                    .parseSignedClaims(oauthToken)
                    .payload
            }.getOrElse { throw OAuthUserInfoFailedException() }

        return UserInfo(
            providerId = claims.subject,
            email = claims["email"] as? String,
        )
    }

    private fun fetchApplePublicKey(idToken: String): RSAPublicKey {
        val idTokenHeader = String(Base64.getUrlDecoder().decode(idToken.split(".")[0]))
        val keyId = objectMapper.readTree(idTokenHeader)["kid"].asText()

        val appleJwks =
            runCatching {
                restClient
                    .get()
                    .uri(jwksUrl)
                    .retrieve()
                    .body<AppleJwks>()
            }.getOrElse { throw OAuthUserInfoFailedException() }
                ?: throw OAuthUserInfoFailedException()

        val matchedKey =
            appleJwks.keys.find { it.kid == keyId }
                ?: throw OAuthUserInfoFailedException()

        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(matchedKey.n))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(matchedKey.e))

        return KeyFactory
            .getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(modulus, exponent)) as RSAPublicKey
    }

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
}
