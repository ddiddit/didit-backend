package com.didit.application.notification.required

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class EmailSenderTest {
    @Mock
    lateinit var emailSender: EmailSender

    @Test
    fun `send`() {
        val to = "test@gmail.com"
        val subject = "테스트 제목"
        val body = "<h1>테스트 내용</h1>"

        emailSender.send(to, subject, body)

        verify(emailSender).send(to, subject, body)
    }
}
