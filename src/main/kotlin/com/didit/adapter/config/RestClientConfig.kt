package com.didit.adapter.config

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig {
    // OpenAI 등 외부 호출 타임아웃. 설정이 없으면 OpenAI 지연 시 서버 스레드가 무한정 묶임.
    // 읽기 타임아웃은 AI 요약 생성이 오래 걸릴 수 있어 넉넉히 두되, 무한 대기는 막는다.
    @Bean
    fun restClient(): RestClient {
        val settings =
            ClientHttpRequestFactorySettings
                .defaults()
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofSeconds(60))

        return RestClient
            .builder()
            .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
            .build()
    }
}
