package com.didit.adapter.integration.speech

import com.didit.adapter.config.ClovaSpeechProperties
import com.didit.application.speech.exception.SpeechTranscriptionFailedException
import com.didit.application.speech.required.SpeechClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.multipart.MultipartFile

@Component
class ClovaSpeechClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val clovaSpeechProperties: ClovaSpeechProperties,
) : SpeechClient {
    override fun transcribe(file: MultipartFile): String {
        val requestBody = LinkedMultiValueMap<String, Any>()
        requestBody.add("media", createMediaPart(file))
        requestBody.add(
            "params",
            HttpEntity(
                """
                {
                  "language": "ko-KR",
                  "completion": "sync"
                }
                """.trimIndent(),
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                },
            ),
        )

        val response =
            restClient
                .post()
                .uri(clovaSpeechProperties.invokeUrl)
                .header("X-CLOVASPEECH-API-KEY", clovaSpeechProperties.secretKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(requestBody)
                .retrieve()
                .body<String>()
                ?: throw SpeechTranscriptionFailedException("CLOVA Speech 응답을 받지 못했습니다.")

        val result =
            runCatching {
                objectMapper.readValue(response, ClovaSpeechResponse::class.java)
            }.getOrElse {
                throw SpeechTranscriptionFailedException("CLOVA Speech 응답 파싱에 실패했습니다. response: $response")
            }

        if (result.result != "COMPLETED") {
            throw SpeechTranscriptionFailedException("result: ${result.result}, message: ${result.message}")
        }

        return result.text
    }

    private fun createMediaPart(file: MultipartFile): HttpEntity<ByteArrayResource> =
        HttpEntity(
            object : ByteArrayResource(file.bytes) {
                override fun getFilename(): String = file.originalFilename ?: "voice.wav"
            },
            HttpHeaders().apply {
                contentType = MediaType.parseMediaType(file.contentType ?: "audio/wav")
                contentDisposition =
                    ContentDisposition
                        .builder("form-data")
                        .name("media")
                        .filename(file.originalFilename ?: "voice.wav")
                        .build()
            },
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClovaSpeechResponse(
    val result: String,
    val message: String,
    val text: String,
)