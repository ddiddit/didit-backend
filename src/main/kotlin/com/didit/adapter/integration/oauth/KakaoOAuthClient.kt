package com.didit.adapter.integration.oauth

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.required.OAuthClient
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class KakaoOAuthClient(
    private val restClient: RestClient,
    @param:Value("\${oauth.kakao.user-info-url}") private val userInfoUrl: String,
) : OAuthClient {
    override fun getUserInfo(oauthToken: String): UserInfo {
        val kakaoUser =
            runCatching {
                restClient
                    .get()
                    .uri(userInfoUrl)
                    .header("Authorization", "Bearer $oauthToken")
                    .retrieve()
                    .body<KakaoUserResponse>()
            }.getOrElse { throw OAuthUserInfoFailedException() }
                ?: throw OAuthUserInfoFailedException()

        return UserInfo(
            providerId = kakaoUser.id.toString(),
            email = kakaoUser.kakaoAccount?.email,
        )
    }

    private data class KakaoUserResponse(
        val id: Long,
        @param:JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?,
    )

    private data class KakaoAccount(
        val email: String?,
    )
}
