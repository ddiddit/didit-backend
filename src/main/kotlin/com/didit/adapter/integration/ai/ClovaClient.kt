package com.didit.adapter.integration.ai

import com.didit.adapter.integration.ai.FeedbackPrompts
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.required.AIClient
import com.didit.domain.shared.Job
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class ClovaClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @param:Value("\${clova.api.url}") private val apiUrl: String,
    @param:Value("\${clova.api.api-key}") private val apiKey: String,
) : AIClient {
    override fun generateDeepQuestion(
        job: Job?,
        answers: List<String>,
    ): String {
        val prompt = FeedbackPrompts.buildDeepQuestionPrompt(job, answers)
        val response = callClova(prompt)
        return runCatching {
            data class DeepQuestionDto(
                val question: String,
            )
            objectMapper.readValue<DeepQuestionDto>(response).question
        }.getOrElse {
            throw RuntimeException("심화 질문 파싱에 실패했습니다. response: $response")
        }
    }

    override fun generateSummaryWithTitle(
        job: Job?,
        allAnswers: List<String>,
    ): AISummaryResponse {
        val prompt = FeedbackPrompts.buildSummaryPrompt(job, allAnswers)
        val response = callClova(prompt)
        return runCatching {
            objectMapper.readValue<AISummaryResponse>(response)
        }.getOrElse {
            throw RuntimeException("회고 요약 파싱에 실패했습니다. response: $response")
        }
    }

    private fun callClova(prompt: String): String {
        val request =
            ClovaRequest(
                messages =
                    listOf(
                        ClovaMessage(role = "system", content = "당신은 전문 회고 코치입니다."),
                        ClovaMessage(role = "user", content = prompt),
                    ),
                maxTokens = 500,
                temperature = 0.7,
                topP = 0.8,
                repetitionPenalty = 1.1,
                topK = 0,
            )

        return restClient
            .post()
            .uri(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<ClovaResponse>()
            ?.result
            ?.message
            ?.content
            ?: throw RuntimeException("Clova 응답을 받지 못했습니다.")
    }
}

data class ClovaRequest(
    val messages: List<ClovaMessage>,
    val maxTokens: Int,
    val temperature: Double,
    val topP: Double,
    val repetitionPenalty: Double,
    val topK: Int = 0,
)

data class ClovaMessage(
    val role: String,
    val content: String,
)

data class ClovaResponse(
    val result: ClovaResult,
)

data class ClovaResult(
    val message: ClovaMessage,
    val stopReason: String,
    val inputLength: Int,
    val outputLength: Int,
)
