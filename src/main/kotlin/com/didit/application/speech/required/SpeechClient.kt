package com.didit.application.speech.required

import org.springframework.web.multipart.MultipartFile

interface SpeechClient {
    fun transcribe(file: MultipartFile): String
}
