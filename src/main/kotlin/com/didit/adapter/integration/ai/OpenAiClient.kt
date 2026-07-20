package com.didit.adapter.integration.ai

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.GeneratedDeepQuestion
import com.didit.domain.shared.Job
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class OpenAiClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val feedbackPrompts: FeedbackPrompts,
    private val metrics: OpenAiMetrics,
    @param:Value("\${openai.api-key}") private val apiKey: String,
    @param:Value("\${openai.chat.model}") private val model: String,
) : AIClient {
    companion object {
        private const val URL = "https://api.openai.com/v1/responses"
        private val logger = LoggerFactory.getLogger(OpenAiClient::class.java)
        private const val SYSTEM_PROMPT = "당신은 회고 전문 코치입니다."
    }

    override fun generateDeepQuestion(
        job: Job?,
        answers: List<String>,
    ): GeneratedDeepQuestion {
        val prompt = feedbackPrompts.buildDeepQuestionPrompt(job, answers)

        logger.debug("심화 질문 프롬프트 - job: $job, prompt:\n$prompt")

        val result = callWithResult(prompt, "deep_question", "deep_question", deepQuestionSchema())

        return parseDeepQuestion(result)
    }

    override fun generateSummaryWithTitle(
        job: Job?,
        allAnswers: List<String>,
        deepQuestion: String?,
    ): AISummaryResponse {
        val prompt = feedbackPrompts.buildSummaryPrompt(job, allAnswers, deepQuestion)

        logger.debug("요약 프롬프트 - job: $job, prompt:\n$prompt")

        val result = callWithResult(prompt, "summary", "retrospective_summary", summarySchema())

        return parseSummary(result)
    }

    private fun callWithResult(
        prompt: String,
        operation: String,
        schemaName: String,
        schema: Map<String, Any>,
    ): OpenAiResponse {
        metrics.recordPromptCharacters(operation, prompt.length)
        logger.info(
            "OpenAI request started - operation: {}, transactionActive: {}",
            operation,
            TransactionSynchronizationManager.isActualTransactionActive(),
        )

        return metrics.record(operation) {
            val rawResponse =
                restClient
                    .post()
                    .uri(URL)
                    .header("Authorization", "Bearer $apiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        OpenAiRequest(
                            model = model,
                            instructions = SYSTEM_PROMPT,
                            input = prompt,
                            maxOutputTokens = 3000,
                            text =
                                OpenAiTextFormat(
                                    format =
                                        OpenAiJsonSchemaFormat(
                                            name = schemaName,
                                            schema = schema,
                                        ),
                                ),
                        ),
                    ).retrieve()
                    .body<String>() ?: throw RuntimeException("OpenAI 응답을 받지 못했습니다.")

            logger.debug("OpenAI 전체 응답: $rawResponse")

            objectMapper.readValue<OpenAiResponse>(rawResponse).also {
                metrics.recordTokens(operation, it.usage?.inputTokens ?: 0, it.usage?.outputTokens ?: 0)
            }
        }
    }

    private fun parseDeepQuestion(response: OpenAiResponse): GeneratedDeepQuestion =
        runCatching {
            val question = objectMapper.readValue<DeepQuestionDto>(response.outputText).question

            logger.debug("심화 질문 토큰 사용량 - inputTokens: ${response.usage?.inputTokens}, outputTokens: ${response.usage?.outputTokens}")

            GeneratedDeepQuestion(
                content = question,
                inputTokens = response.usage?.inputTokens ?: 0,
                outputTokens = response.usage?.outputTokens ?: 0,
            )
        }.getOrElse {
            logger.warn("심화 질문 JSON 파싱 실패, 텍스트 그대로 사용. response: ${response.outputText}")

            GeneratedDeepQuestion(
                content =
                    response.outputText
                        .trim()
                        .removeSurrounding("\""),
                inputTokens = response.usage?.inputTokens ?: 0,
                outputTokens = response.usage?.outputTokens ?: 0,
            )
        }

    private fun parseSummary(response: OpenAiResponse): AISummaryResponse =
        runCatching {
            logger.debug("회고 요약 토큰 사용량 - inputTokens: ${response.usage?.inputTokens}, outputTokens: ${response.usage?.outputTokens}")

            objectMapper.readValue<AISummaryResponse>(response.outputText).copy(
                inputTokens = response.usage?.inputTokens ?: 0,
                outputTokens = response.usage?.outputTokens ?: 0,
            )
        }.getOrElse {
            throw RuntimeException("회고 요약 파싱에 실패했습니다. response: ${response.outputText}")
        }

    private fun deepQuestionSchema() =
        mapOf(
            "type" to "object",
            "properties" to
                mapOf(
                    "question" to
                        mapOf(
                            "type" to "string",
                            "description" to "사용자 답변을 바탕으로 생성한 심화 질문",
                        ),
                ),
            "required" to listOf("question"),
            "additionalProperties" to false,
        )

    private fun summarySchema() =
        mapOf(
            "type" to "object",
            "properties" to
                mapOf(
                    "title" to mapOf("type" to "string"),
                    "summary" to mapOf("type" to "string"),
                    "blockedPoint" to stringArraySchema(),
                    "solutionProcess" to stringArraySchema(),
                    "lessonLearned" to stringArraySchema(),
                    "insight" to titledDescriptionSchema(),
                    "nextAction" to titledDescriptionSchema(),
                ),
            "required" to
                listOf(
                    "title",
                    "summary",
                    "blockedPoint",
                    "solutionProcess",
                    "lessonLearned",
                    "insight",
                    "nextAction",
                ),
            "additionalProperties" to false,
        )

    private fun stringArraySchema() =
        mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string"),
        )

    private fun titledDescriptionSchema() =
        mapOf(
            "type" to "object",
            "properties" to
                mapOf(
                    "title" to mapOf("type" to "string"),
                    "description" to mapOf("type" to "string"),
                ),
            "required" to listOf("title", "description"),
            "additionalProperties" to false,
        )
}

private data class DeepQuestionDto(
    val question: String,
)

private data class OpenAiRequest(
    val model: String,
    val instructions: String,
    val input: String,
    @JsonProperty("max_output_tokens")
    val maxOutputTokens: Int,
    val text: OpenAiTextFormat,
)

private data class OpenAiTextFormat(
    val format: OpenAiJsonSchemaFormat,
)

private data class OpenAiJsonSchemaFormat(
    val type: String = "json_schema",
    val name: String,
    val strict: Boolean = true,
    val schema: Map<String, Any>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiResponse(
    val output: List<OpenAiOutput> = emptyList(),
    val usage: OpenAiUsage? = null,
) {
    val outputText: String
        get() =
            output
                .flatMap { it.content }
                .firstOrNull { it.type == "output_text" }
                ?.text
                ?: throw RuntimeException("OpenAI 응답 텍스트를 찾지 못했습니다.")
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiOutput(
    val content: List<OpenAiOutputContent> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiOutputContent(
    val type: String,
    val text: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiUsage(
    @JsonProperty("input_tokens")
    val inputTokens: Int = 0,
    @JsonProperty("output_tokens")
    val outputTokens: Int = 0,
)
