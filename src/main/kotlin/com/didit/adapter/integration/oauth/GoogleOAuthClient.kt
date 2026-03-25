package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.application.auth.required.OAuthClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class GoogleOAuthClient(
    private val restClient: RestClient,
    @param:Value("\${oauth.google.token-info-url}") private val tokenInfoUrl: String,
) : OAuthClient {
    override fun getUserInfo(oauthToken: String): UserInfo {
        val googleTokenInfo =
            runCatching {
                restClient
                    .get()
                    .uri("$tokenInfoUrl?id_token=$oauthToken")
                    .retrieve()
                    .body<GoogleTokenInfo>()
            }.getOrElse { throw OAuthUserInfoFailedException() }
                ?: throw OAuthUserInfoFailedException()

        return UserInfo(
            providerId = googleTokenInfo.sub,
            email = googleTokenInfo.email,
        )
    }

    private data class GoogleTokenInfo(
        val sub: String,
        val email: String?,
    )
}
