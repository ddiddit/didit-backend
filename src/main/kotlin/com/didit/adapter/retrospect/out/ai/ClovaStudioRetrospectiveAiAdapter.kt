package com.didit.adapter.retrospect.out.ai

import com.didit.adapter.retrospect.out.ai.config.ClovaStudioProperties
import com.didit.adapter.retrospect.out.ai.dto.ClovaStudioChatMessageDto
import com.didit.adapter.retrospect.out.ai.dto.ClovaStudioChatRequest
import com.didit.adapter.retrospect.out.ai.dto.ClovaStudioChatResponse
import com.didit.adapter.retrospect.out.ai.prompt.RetrospectiveAiPromptFactory
import com.didit.application.retrospect.port.out.AiAnalyzeResult
import com.didit.application.retrospect.port.out.AiDeepQuestionResult
import com.didit.application.retrospect.port.out.AiSummaryResult
import com.didit.application.retrospect.port.out.RetrospectiveAiPort
import com.didit.domain.retrospect.entity.ChatMessage
import com.didit.domain.retrospect.model.RetrospectiveSummary
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Primary
@Component
class ClovaStudioRetrospectiveAiAdapter(
    private val clovaStudioRestClient: RestClient,
    private val clovaStudioProperties: ClovaStudioProperties,
    private val promptFactory: RetrospectiveAiPromptFactory,
    private val objectMapper: ObjectMapper,
) : RetrospectiveAiPort {
    override fun analyzeAnswer(messages: List<ChatMessage>): AiAnalyzeResult {
        val content =
            callChat(
                promptFactory.buildAnalyzeMessages(messages),
                maxTokens = 100,
                temperature = 0.1,
            )

        return AiAnalyzeResult(
            inputTokens = estimateInputTokens(messages),
            outputTokens = estimateOutputTokens(content),
        )
    }

    override fun generateDeepQuestion(messages: List<ChatMessage>): AiDeepQuestionResult {
        val content =
            callChat(
                promptFactory.buildDeepQuestionMessages(messages),
                maxTokens = 150,
                temperature = 0.5,
            )

        return AiDeepQuestionResult(
            question = content.trim(),
            inputTokens = estimateInputTokens(messages),
            outputTokens = estimateOutputTokens(content),
        )
    }

    override fun generateSummary(messages: List<ChatMessage>): AiSummaryResult {
        val content =
            callChat(
                promptFactory.buildSummaryMessages(messages),
                maxTokens = 800,
                temperature = 0.3,
            )

        val summary = objectMapper.readValue(content, RetrospectiveSummary::class.java)

        return AiSummaryResult(
            summary = summary,
            inputTokens = estimateInputTokens(messages),
            outputTokens = estimateOutputTokens(content),
        )
    }

    private fun callChat(
        promptMessages: List<Pair<String, String>>,
        maxTokens: Int,
        temperature: Double,
    ): String {
        val request =
            ClovaStudioChatRequest(
                messages =
                    promptMessages.map {
                        ClovaStudioChatMessageDto(
                            role = it.first,
                            content = it.second,
                        )
                    },
                maxTokens = maxTokens,
                temperature = temperature,
            )

        val response =
            clovaStudioRestClient
                .post()
                .uri("/v3/chat-completions/${clovaStudioProperties.model}")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer ${clovaStudioProperties.apiKey}")
                .header("X-NCP-CLOVASTUDIO-REQUEST-ID", clovaStudioProperties.requestId)
                .body(request)
                .retrieve()
                .body(ClovaStudioChatResponse::class.java)
                ?: throw IllegalStateException("CLOVA Studio 응답이 없습니다.")

        return response.result?.message?.content
            ?: throw IllegalStateException("CLOVA Studio 응답 content가 없습니다.")
    }

    private fun estimateInputTokens(messages: List<ChatMessage>): Int = messages.sumOf { it.content.length / 2 + 1 }

    private fun estimateOutputTokens(content: String): Int = content.length / 2 + 1
}
