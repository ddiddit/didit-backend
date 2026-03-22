package com.didit.adapter.auth.social

import com.didit.adapter.webapi.auth.dto.KakaoTokenResponse
import com.didit.application.auth.required.social.KakaoAuthPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Component
class KakaoAuthAdapter(
    private val restTemplate: RestTemplate,
    @Value("\${oauth.kakao.client-id}")
    private val clientId: String,
    @Value("\${oauth.kakao.client-secret}")
    private val clientSecret: String,
) : KakaoAuthPort {
    override fun getIdToken(
        code: String,
        redirectUri: String,
    ): String {
        val url = "https://kauth.kakao.com/oauth/token"

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            }

        val body: MultiValueMap<String, String> =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", clientId)
                add("client_secret", clientSecret)
                add("redirect_uri", redirectUri)
                add("code", code)
            }

        val request = HttpEntity(body, headers)

        val response =
            restTemplate.postForEntity(
                url,
                request,
                KakaoTokenResponse::class.java,
            )

        val responseBody =
            response.body
                ?: throw IllegalArgumentException("Kakao token 요청 실패")

        return responseBody.idToken
            ?: throw IllegalArgumentException("id_token 없음 (scope=openid 확인)")
    }
}
