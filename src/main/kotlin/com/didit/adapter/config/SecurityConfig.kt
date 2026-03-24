package com.didit.adapter.config

import com.didit.adapter.auth.jwt.JwtFilter
import com.didit.adapter.auth.jwt.JwtProvider
import com.didit.adapter.auth.security.CustomUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val customUserDetailsService: CustomUserDetailsService,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val jwtFilter = JwtFilter(jwtProvider, customUserDetailsService)

        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests {
                it
                    .requestMatchers(
                        "api/v1/auth/**",
                        "/actuator/health",
                        "/docs/app/*",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}
