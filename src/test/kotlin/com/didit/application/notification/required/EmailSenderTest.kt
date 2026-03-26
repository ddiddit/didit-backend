package com.didit.application.notification.required

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.springframework.core.io.ClassPathResource

@ExtendWith(MockitoExtension::class)
class EmailSenderTest {
    @Mock
    lateinit var emailSender: EmailSender

    @Test
    fun `이미지 없이 이메일 전송`() {
        val to = "test@gmail.com"
        val subject = "테스트 제목"
        val body = "<h1>테스트 내용</h1>"

        emailSender.send(to, subject, body)

        verify(emailSender).send(to, subject, body)
    }

    @Test
    fun `인라인 이미지 포함하여 이메일 전송`() {
        val to = "test@gmail.com"
        val subject = "테스트 제목"
        val body = "<h1>테스트 내용</h1>"
        val inlineImages =
            mapOf(
                "logo" to ClassPathResource("static/images/logo.png"),
                "character" to ClassPathResource("static/images/character.png"),
            )

        emailSender.send(to, subject, body, inlineImages)

        verify(emailSender).send(to, subject, body, inlineImages)
    }
}
