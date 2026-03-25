package com.didit.adapter.config

import com.didit.adapter.integration.oauth.AppleOAuthClient
import com.didit.adapter.integration.oauth.GoogleOAuthClient
import com.didit.adapter.integration.oauth.KakaoOAuthClient
import com.didit.application.auth.required.OAuthClientFactory
import com.didit.domain.auth.Provider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OAuthClientConfig(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val googleOAuthClient: GoogleOAuthClient,
    private val appleOAuthClient: AppleOAuthClient,
) {
    @Bean
    fun oAuthClientFactory(): OAuthClientFactory =
        OAuthClientFactory(
            mapOf(
                Provider.KAKAO to kakaoOAuthClient,
                Provider.GOOGLE to googleOAuthClient,
                Provider.APPLE to appleOAuthClient,
            ),
        )
}
