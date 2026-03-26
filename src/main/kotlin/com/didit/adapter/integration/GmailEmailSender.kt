package com.didit.adapter.integration

import com.didit.application.notification.required.EmailSender
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class GmailEmailSender(
    private val mailSender: JavaMailSender,
) : EmailSender {
    override fun send(
        to: String,
        subject: String,
        body: String,
    ) {
        val message = mailSender.createMimeMessage()

        MimeMessageHelper(message, true, "UTF-8").apply {
            setTo(to)
            setSubject(subject)
            setText(body, true)
        }

        mailSender.send(message)
    }
}
