package com.didit.application.retrospect.provided

import org.springframework.web.multipart.MultipartFile

interface SpeechTranscriber {
    fun transcribe(file: MultipartFile): String
}
