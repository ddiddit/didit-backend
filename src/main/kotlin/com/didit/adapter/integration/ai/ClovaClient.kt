package com.didit.adapter.integration.ai

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.GeneratedDeepQuestion
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
    companion object {
        private const val SYSTEM_PROMPT = "당신은 전문 회고 코치입니다."
    }

    override fun generateDeepQuestion(
        job: Job?,
        answers: List<String>,
    ): GeneratedDeepQuestion {
        val prompt = FeedbackPrompts.buildDeepQuestionPrompt(job, answers)
        val result = callWithResult(prompt)
        return parseDeepQuestion(result)
    }

    override fun generateSummaryWithTitle(
        job: Job?,
        allAnswers: List<String>,
    ): AISummaryResponse {
        val prompt = FeedbackPrompts.buildSummaryPrompt(job, allAnswers)
        val result = callWithResult(prompt)
        return parseSummary(result)
    }

    private fun callWithResult(prompt: String): ClovaResult {
        val request =
            ClovaRequest(
                messages =
                    listOf(
                        ClovaMessage(role = "system", content = SYSTEM_PROMPT),
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
            ?: throw RuntimeException("Clova 응답을 받지 못했습니다.")
    }

    private fun parseDeepQuestion(result: ClovaResult): GeneratedDeepQuestion =
        runCatching {
            data class DeepQuestionDto(
                val question: String,
            )

            val question = objectMapper.readValue<DeepQuestionDto>(result.message.content).question

            GeneratedDeepQuestion(
                content = question,
                inputTokens = result.inputLength,
                outputTokens = result.outputLength,
            )
        }.getOrElse {
            throw RuntimeException("심화 질문 파싱에 실패했습니다. response: ${result.message.content}")
        }

    private fun parseSummary(result: ClovaResult): AISummaryResponse =
        runCatching {
            val cleanResponse =
                result.message.content
                    .replace(Regex("```json\\s*"), "")
                    .replace(Regex("```\\s*$"), "")
                    .trim()

            objectMapper.readValue<AISummaryResponse>(cleanResponse).copy(
                inputTokens = result.inputLength,
                outputTokens = result.outputLength,
            )
        }.getOrElse {
            throw RuntimeException("회고 요약 파싱에 실패했습니다. response: ${result.message.content}")
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
    val stopReason: String?,
    val inputLength: Int,
    val outputLength: Int,
)
