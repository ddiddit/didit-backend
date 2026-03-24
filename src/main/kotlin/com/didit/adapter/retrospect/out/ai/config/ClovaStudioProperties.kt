package com.didit.adapter.retrospect.out.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clova.studio")
data class ClovaStudioProperties(
    val baseUrl: String,
    val apiKey: String,
    val requestId: String,
    val model: String,
)
