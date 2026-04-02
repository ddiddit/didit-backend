package com.didit.application.retrospect

import com.didit.application.retrospect.exception.SpeechEmptyFileException
import com.didit.application.retrospect.exception.SpeechEmptyResultException
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.required.SpeechClient
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class SpeechService(
    private val speechClient: SpeechClient,
) {
    fun transcribe(file: MultipartFile): String {
        val filename =
            file.originalFilename
                ?: throw SpeechUnsupportedFileException(null, file.contentType)
        if (file.isEmpty) throw SpeechEmptyFileException()
        if (!filename.lowercase().endsWith(".wav")) throw SpeechUnsupportedFileException(filename, file.contentType)

        val text = speechClient.transcribe(file.bytes, filename).trim()
        if (text.isBlank()) throw SpeechEmptyResultException()

        return text
    }
}
