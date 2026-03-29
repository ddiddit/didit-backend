package com.didit.adapter.webapi.speech

import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.speech.dto.SpeechToTextResponse
import com.didit.application.speech.provided.SpeechTranscriber
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/v1/speech-to-text")
@RestController
class SpeechApi(
    private val speechTranscriber: SpeechTranscriber,
) {
    @RequireAuth
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun transcribe(
        @RequestPart("file") file: MultipartFile,
    ): SuccessResponse<SpeechToTextResponse> {
        val text = speechTranscriber.transcribe(file)

        return SuccessResponse.of(SpeechToTextResponse(text))
    }
}