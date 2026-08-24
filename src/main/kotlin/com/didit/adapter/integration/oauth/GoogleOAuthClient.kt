package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.InvalidSocialCredentialTypeException
import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.application.auth.required.OAuthClient
import com.didit.domain.auth.SocialCredentialType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant

@Component
class GoogleOAuthClient(
    private val restClient: RestClient,
    @param:Value("\${oauth.google.token-info-url}") private val tokenInfoUrl: String,
    @param:Value("\${oauth.google.allowed-client-ids:}") allowedClientIds: String,
) : OAuthClient {
    private val allowedClientIds =
        allowedClientIds
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()

    override fun getUserInfo(oauthToken: String): UserInfo = verifyIdToken(oauthToken)

    override fun getUserInfo(
        credentialType: SocialCredentialType,
        credential: String,
    ): UserInfo {
        if (credentialType != SocialCredentialType.ID_TOKEN) {
            throw InvalidSocialCredentialTypeException()
        }
        return verifyIdToken(credential)
    }

    private fun verifyIdToken(idToken: String): UserInfo {
        val googleTokenInfo =
            runCatching {
                restClient
                    .get()
                    .uri(
                        UriComponentsBuilder
                            .fromUriString(tokenInfoUrl)
                            .queryParam("id_token", idToken)
                            .build()
                            .toUri(),
                    ).retrieve()
                    .body<GoogleTokenInfo>()
            }.getOrElse { throw OAuthUserInfoFailedException() }
                ?: throw OAuthUserInfoFailedException()

        val issuerValid = googleTokenInfo.iss == "accounts.google.com" || googleTokenInfo.iss == "https://accounts.google.com"
        val audienceValid = allowedClientIds.isNotEmpty() && googleTokenInfo.aud in allowedClientIds
        val expirationValid = googleTokenInfo.exp.toLongOrNull()?.let { it > Instant.now().epochSecond } == true
        if (!issuerValid || !audienceValid || !expirationValid) throw OAuthUserInfoFailedException()

        return UserInfo(
            providerId = googleTokenInfo.sub,
            email = googleTokenInfo.email,
        )
    }

    private data class GoogleTokenInfo(
        val sub: String,
        val email: String?,
        val aud: String,
        val iss: String,
        val exp: String,
    )
}
