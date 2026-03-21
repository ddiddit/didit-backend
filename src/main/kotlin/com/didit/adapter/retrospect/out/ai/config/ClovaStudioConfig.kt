package com.didit.adapter.retrospect.out.ai.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(ClovaStudioProperties::class)
class ClovaStudioConfig {

    @Bean
    fun clovaStudioRestClient(
        properties: ClovaStudioProperties
    ): RestClient {
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .build()
    }
}