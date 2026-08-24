package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.InvalidSocialCredentialTypeException
import com.didit.application.auth.exception.OAuthUserInfoFailedException
import com.didit.application.auth.required.OAuthClient
import com.didit.domain.auth.SocialCredentialType
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class KakaoOAuthClient(
    private val restClient: RestClient,
    @param:Value("\${oauth.kakao.user-info-url}") private val userInfoUrl: String,
    @param:Value("\${oauth.kakao.token-url}") private val tokenUrl: String,
    @param:Value("\${oauth.kakao.token-info-url}") private val tokenInfoUrl: String,
    @param:Value("\${oauth.kakao.rest-api-key:}") private val restApiKey: String,
    @param:Value("\${oauth.kakao.client-secret:}") private val clientSecret: String,
    @param:Value("\${oauth.kakao.redirect-uri}") private val redirectUri: String,
    @param:Value("\${oauth.kakao.app-id:}") private val appId: Long?,
) : OAuthClient {
    override fun getUserInfo(oauthToken: String): UserInfo = getUserInfoWithAccessToken(oauthToken)

    override fun getUserInfo(
        credentialType: SocialCredentialType,
        credential: String,
    ): UserInfo =
        when (credentialType) {
            SocialCredentialType.ACCESS_TOKEN -> getUserInfoWithAccessToken(credential)
            SocialCredentialType.AUTHORIZATION_CODE -> getUserInfoWithAccessToken(exchangeAuthorizationCode(credential))
            else -> throw InvalidSocialCredentialTypeException()
        }

    private fun exchangeAuthorizationCode(code: String): String {
        if (restApiKey.isBlank() || clientSecret.isBlank()) throw OAuthUserInfoFailedException()

        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", restApiKey)
                add("client_secret", clientSecret)
                add("redirect_uri", redirectUri)
                add("code", code)
            }

        return runCatching {
            restClient
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body<KakaoTokenResponse>()
                ?.accessToken
        }.getOrElse { throw OAuthUserInfoFailedException() }
            ?: throw OAuthUserInfoFailedException()
    }

    private fun getUserInfoWithAccessToken(accessToken: String): UserInfo {
        verifyAppId(accessToken)

        val kakaoUser =
            runCatching {
                restClient
                    .get()
                    .uri(userInfoUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .retrieve()
                    .body<KakaoUserResponse>()
            }.getOrElse { throw OAuthUserInfoFailedException() }
                ?: throw OAuthUserInfoFailedException()

        return UserInfo(
            providerId = kakaoUser.id.toString(),
            email = kakaoUser.kakaoAccount?.email,
        )
    }

    private fun verifyAppId(accessToken: String) {
        val expectedAppId = appId ?: throw OAuthUserInfoFailedException()
        val tokenInfo =
            runCatching {
                restClient
                    .get()
                    .uri(tokenInfoUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .retrieve()
                    .body<KakaoTokenInfoResponse>()
            }.getOrElse { throw OAuthUserInfoFailedException() }
                ?: throw OAuthUserInfoFailedException()

        if (tokenInfo.appId != expectedAppId) throw OAuthUserInfoFailedException()
    }

    private data class KakaoTokenResponse(
        @param:JsonProperty("access_token") val accessToken: String,
    )

    private data class KakaoTokenInfoResponse(
        @param:JsonProperty("app_id") val appId: Long,
    )

    private data class KakaoUserResponse(
        val id: Long,
        @param:JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?,
    )

    private data class KakaoAccount(
        val email: String?,
    )
}
