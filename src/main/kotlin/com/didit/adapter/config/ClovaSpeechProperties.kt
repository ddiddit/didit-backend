package com.didit.adapter.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clova.speech")
data class ClovaSpeechProperties(
    val invokeUrl: String,
    val secretKey: String,
)
