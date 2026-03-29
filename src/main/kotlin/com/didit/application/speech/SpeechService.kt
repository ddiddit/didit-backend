package com.didit.application.speech

import com.didit.application.speech.exception.SpeechEmptyFileException
import com.didit.application.speech.exception.SpeechEmptyResultException
import com.didit.application.speech.exception.SpeechUnsupportedFileException
import com.didit.application.speech.provided.SpeechTranscriber
import com.didit.application.speech.required.SpeechClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Transactional(readOnly = true)
@Service
class SpeechService(
    private val speechClient: SpeechClient,
) : SpeechTranscriber {
    override fun transcribe(file: MultipartFile): String {
        if (file.isEmpty) {
            throw SpeechEmptyFileException()
        }

        if (!isWavFile(file)) {
            throw SpeechUnsupportedFileException(file.originalFilename, file.contentType)
        }

        val text = speechClient.transcribe(file).trim()
        if (text.isBlank()) {
            throw SpeechEmptyResultException()
        }

        return text
    }

    private fun isWavFile(file: MultipartFile): Boolean {
        val originalFilename = file.originalFilename ?: return false
        return originalFilename.lowercase().endsWith(".wav")
    }
}
