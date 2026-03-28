package com.didit.adapter.integration.ai

import com.didit.adapter.integration.ai.prompt.ClovaPrompts
import com.didit.application.retrospect.required.AIClient
import com.didit.domain.auth.Job
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ClovaClient(
    private val webClient: WebClient,
    @Value("\${clova.api.api-key}") private val apiKey: String,
    @Value("\${clova.api.request-id}") private val requestId: String,
) : AIClient {
    companion object {
        private const val CLOVA_API_URL = "https://clovastudio.stream.ntruss.com/v1/chat-completions/HCX-003"
    }

    override fun generateDeepQuestion(
        job: Job,
        answers: List<String>,
    ): String {
        val prompt = ClovaPrompts.buildDeepQuestionPrompt(job, answers)
        return callClova(prompt)
    }

    override fun generateSummary(
        job: Job,
        allAnswers: List<String>,
    ): String {
        val prompt = ClovaPrompts.buildSummaryPrompt(job, allAnswers)
        return callClova(prompt)
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
                repeatPenalty = 5.0,
                topK = 0,
                includeAiFilters = false,
            )

        return webClient
            .post()
            .uri(CLOVA_API_URL)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "Bearer $apiKey")
            .header("X-NCP-CLOVASTUDIO-REQUEST-ID", requestId)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(request)
            .retrieve()
            .bodyToMono<ClovaResponse>()
            .map { it.result.message.content }
            .block()
            ?: throw RuntimeException("Clova 응답을 받지 못했습니다.")
    }
}

data class ClovaRequest(
    val messages: List<ClovaMessage>,
    val maxTokens: Int,
    val temperature: Double,
    val topP: Double,
    val repeatPenalty: Double,
    val topK: Int = 0,
    val includeAiFilters: Boolean = false,
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
