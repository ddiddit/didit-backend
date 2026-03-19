package com.didit.adapter.webapi.auth

import com.didit.application.auth.provided.RefreshTokenUseCase
import com.didit.application.auth.provided.SocialLoginUseCase
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
class AuthControllerTestConfig {
    @Bean
    fun socialLoginUseCase(): SocialLoginUseCase = mock(SocialLoginUseCase::class.java)

    @Bean
    fun refreshTokenUseCase(): RefreshTokenUseCase = mock(RefreshTokenUseCase::class.java)

    @Bean
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}
