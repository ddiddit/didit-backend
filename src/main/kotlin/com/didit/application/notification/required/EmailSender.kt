package com.didit.application.notification.required

interface EmailSender {
    fun send(
        to: String,
        subject: String,
        body: String,
    )
}
