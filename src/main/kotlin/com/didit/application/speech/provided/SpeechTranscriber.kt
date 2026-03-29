package com.didit.application.speech.provided

import org.springframework.web.multipart.MultipartFile

interface SpeechTranscriber {
    fun transcribe(file: MultipartFile): String
}
