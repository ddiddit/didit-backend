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
class OpenAiSpeechClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @param:Value("\${openai.api-key}") private val apiKey: String,
    @param:Value("\${openai.speech.model}") private val model: String,
) : SpeechClient {
    companion object {
        private const val TRANSCRIPTIONS_URL = "https://api.openai.com/v1/audio/transcriptions"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val FILE_PART_NAME = "file"
        private const val MODEL_PART_NAME = "model"
        private const val LANGUAGE_PART_NAME = "language"
        private const val LANGUAGE = "ko"
    }

    override fun transcribe(
        audioBytes: ByteArray,
        filename: String,
    ): String {
        val response = requestTranscription(audioBytes, filename)
        val result = parseResponse(response)
        return result.text
    }

    private fun requestTranscription(
        audioBytes: ByteArray,
        filename: String,
    ): String {
        val requestBody =
            LinkedMultiValueMap<String, Any>().apply {
                add(FILE_PART_NAME, createFilePart(audioBytes, filename))
                add(MODEL_PART_NAME, model)
                add(LANGUAGE_PART_NAME, LANGUAGE)
            }

        return restClient
            .post()
            .uri(TRANSCRIPTIONS_URL)
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$apiKey")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(requestBody)
            .retrieve()
            .body<String>()
            ?: throw SpeechTranscriptionFailedException("OpenAI STT 응답을 받지 못했습니다.")
    }

    private fun parseResponse(response: String): OpenAiSpeechResponse =
        runCatching {
            objectMapper.readValue(response, OpenAiSpeechResponse::class.java)
        }.getOrElse {
            throw SpeechTranscriptionFailedException("OpenAI STT 응답 파싱 실패. response: $response")
        }

    private fun createFilePart(
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
                        .name(FILE_PART_NAME)
                        .filename(filename)
                        .build()
            },
        )

    private fun getMediaType(filename: String): MediaType =
        when (filename.substringAfterLast('.', "").lowercase()) {
            "wav" -> MediaType.parseMediaType("audio/wav")
            "m4a" -> MediaType.parseMediaType("audio/m4a")
            "mp3" -> MediaType.parseMediaType("audio/mpeg")
            "mp4" -> MediaType.parseMediaType("audio/mp4")
            "mpeg", "mpga" -> MediaType.parseMediaType("audio/mpeg")
            "ogg" -> MediaType.parseMediaType("audio/ogg")
            "flac" -> MediaType.parseMediaType("audio/flac")
            "webm" -> MediaType.parseMediaType("audio/webm")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiSpeechResponse(
    val text: String,
)
