package com.didit.adapter.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@EnableConfigurationProperties(ClovaSpeechProperties::class)
@Configuration
class RestClientConfig {
    @Bean
    fun restClient(): RestClient = RestClient.create()
}
