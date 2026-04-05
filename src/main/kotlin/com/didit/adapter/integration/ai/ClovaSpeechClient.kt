package com.didit.adapter.integration.ai

import com.didit.application.retrospect.exception.SpeechTranscriptionFailedException
import com.didit.application.retrospect.required.SpeechClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class ClovaSpeechClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @param:Value("\${clova.speech.invoke-url}") private val invokeUrl: String,
    @param:Value("\${clova.speech.secret-key}") private val secretKey: String,
) : SpeechClient {
    companion object {
        private const val LANGUAGE = "ko-KR"
        private const val COMPLETION = "sync"
        private const val API_KEY_HEADER = "X-CLOVASPEECH-API-KEY"
        private const val COMPLETED = "COMPLETED"
    }

    override fun transcribe(
        audioBytes: ByteArray,
        filename: String,
    ): String {
        val response = requestTranscription(audioBytes, filename)
        val result = parseResponse(response)
        validateResult(result)
        return result.text
    }

    private fun requestTranscription(
        audioBytes: ByteArray,
        filename: String,
    ): String {
        val requestBody =
            LinkedMultiValueMap<String, Any>().apply {
                add("media", createMediaPart(audioBytes, filename))
                add("params", createParams())
            }

        return restClient
            .post()
            .uri("$invokeUrl/recognizer/upload")
            .header(API_KEY_HEADER, secretKey)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(requestBody)
            .retrieve()
            .body<String>()
            ?: throw SpeechTranscriptionFailedException("CLOVA Speech 응답을 받지 못했습니다.")
    }

    private fun parseResponse(response: String): ClovaSpeechResponse =
        runCatching {
            objectMapper.readValue(response, ClovaSpeechResponse::class.java)
        }.getOrElse {
            throw SpeechTranscriptionFailedException("CLOVA Speech 응답 파싱 실패. response: $response")
        }

    private fun validateResult(result: ClovaSpeechResponse) {
        if (result.result != COMPLETED) {
            throw SpeechTranscriptionFailedException("result: ${result.result}, message: ${result.message}")
        }
    }

    private fun createParams(): HttpEntity<String> =
        HttpEntity(
            """{ "language": "$LANGUAGE", "completion": "$COMPLETION" }""",
            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON },
        )

    private fun createMediaPart(
        audioBytes: ByteArray,
        filename: String,
    ): HttpEntity<ByteArrayResource> =
        HttpEntity(
            object : ByteArrayResource(audioBytes) {
                override fun getFilename(): String = filename
            },
            HttpHeaders().apply {
                contentType = getMediaType(filename)
                contentDisposition =
                    ContentDisposition
                        .builder("form-data")
                        .name("media")
                        .filename(filename)
                        .build()
            },
        )

    private fun getMediaType(filename: String): MediaType =
        when (filename.substringAfterLast('.', "").lowercase()) {
            "wav" -> MediaType.parseMediaType("audio/wav")
            "m4a" -> MediaType.parseMediaType("audio/m4a")
            "mp3" -> MediaType.parseMediaType("audio/mpeg")
            "aac" -> MediaType.parseMediaType("audio/aac")
            "ogg" -> MediaType.parseMediaType("audio/ogg")
            "flac" -> MediaType.parseMediaType("audio/flac")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClovaSpeechResponse(
    val result: String,
    val message: String,
    val text: String,
)
