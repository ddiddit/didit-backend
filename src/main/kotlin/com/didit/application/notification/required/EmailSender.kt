package com.didit.application.notification.required

import org.springframework.core.io.ClassPathResource

interface EmailSender {
    fun send(
        to: String,
        subject: String,
        body: String,
        inlineImages: Map<String, ClassPathResource> = emptyMap(),
    )
}
