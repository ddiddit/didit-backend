package com.didit.adapter.integration.ai

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.GeneratedDeepQuestion
import com.didit.domain.shared.Job
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
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
        private val logger = LoggerFactory.getLogger(ClovaClient::class.java)
        private const val SYSTEM_PROMPT = "당신은 전문 회고 코치입니다."
    }

    override fun generateDeepQuestion(
        job: Job?,
        answers: List<String>,
    ): GeneratedDeepQuestion {
        val prompt = FeedbackPrompts.buildDeepQuestionPrompt(job, answers)

        logger.debug("심화 질문 프롬프트 - job: $job, prompt:\n$prompt")

        val result = callWithResult(prompt)

        return parseDeepQuestion(result)
    }

    override fun generateSummaryWithTitle(
        job: Job?,
        allAnswers: List<String>,
        deepQuestion: String?,
    ): AISummaryResponse {
        val prompt = FeedbackPrompts.buildSummaryPrompt(job, allAnswers, deepQuestion)

        logger.debug("요약 프롬프트 - job: $job, prompt:\n$prompt")

        val result = callWithResult(prompt)

        return parseSummary(result)
    }

    private fun callWithResult(prompt: String): ClovaResult {
        val rawResponse =
            restClient
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    ClovaRequest(
                        messages =
                            listOf(
                                ClovaMessage(role = "system", content = SYSTEM_PROMPT),
                                ClovaMessage(role = "user", content = prompt),
                            ),
                        maxTokens = 3000,
                        temperature = 0.7,
                        topP = 0.8,
                        repetitionPenalty = 1.1,
                        topK = 0,
                    ),
                ).retrieve()
                .body<String>()

        logger.debug("Clova 전체 응답: $rawResponse")

        return objectMapper
            .readValue<ClovaResponse>(rawResponse!!)
            .result
    }

    private fun cleanJsonResponse(content: String): String =
        content
            .replace(Regex("```json"), "")
            .replace(Regex("```"), "")
            .trim()

    private fun parseDeepQuestion(result: ClovaResult): GeneratedDeepQuestion =
        runCatching {
            data class DeepQuestionDto(
                val question: String,
            )

            val cleanResponse = cleanJsonResponse(result.message.content)
            val question = objectMapper.readValue<DeepQuestionDto>(cleanResponse).question

            logger.debug("심화 질문 토큰 사용량 - promptTokens: ${result.usage.promptTokens}, completionTokens: ${result.usage.completionTokens}")

            GeneratedDeepQuestion(
                content = question,
                inputTokens = result.usage.promptTokens,
                outputTokens = result.usage.completionTokens,
            )
        }.getOrElse {
            throw RuntimeException("심화 질문 파싱에 실패했습니다. response: ${result.message.content}")
        }

    private fun parseSummary(result: ClovaResult): AISummaryResponse =
        runCatching {
            val cleanResponse = cleanJsonResponse(result.message.content)

            logger.debug("회고 요약 토큰 사용량 - promptTokens: ${result.usage.promptTokens}, completionTokens: ${result.usage.completionTokens}")

            objectMapper.readValue<AISummaryResponse>(cleanResponse).copy(
                inputTokens = result.usage.promptTokens,
                outputTokens = result.usage.completionTokens,
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
    val finishReason: String?,
    val usage: ClovaUsage,
)

data class ClovaUsage(
    val promptTokens: Int,
    val completionTokens: Int,
)
