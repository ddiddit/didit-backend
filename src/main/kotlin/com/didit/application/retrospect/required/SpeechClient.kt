package com.didit.application.retrospect.required

import org.springframework.web.multipart.MultipartFile

interface SpeechClient {
    fun transcribe(file: MultipartFile): String
}
