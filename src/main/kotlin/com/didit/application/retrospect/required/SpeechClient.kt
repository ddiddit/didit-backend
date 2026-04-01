package com.didit.application.retrospect.required

interface SpeechClient {
    fun transcribe(
        audioBytes: ByteArray,
        filename: String,
    ): String
}
