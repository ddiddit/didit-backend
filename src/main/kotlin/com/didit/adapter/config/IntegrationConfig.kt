package com.didit.adapter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class IntegrationConfig {
    @Bean
    fun webClient(): WebClient =
        WebClient
            .builder()
            .build()
}
