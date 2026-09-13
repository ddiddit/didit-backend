package com.didit.adapter.integration.ai

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.didit.application.admin.required.AdminPromptPreviewAIClient
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.shared.Job
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.util.UUID

class AdminPromptPreviewOpenAiClientTest {
    @Test
    fun `preview - renders the draft and parses the V2 structured response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val registry = SimpleMeterRegistry()
        val client = createClient(builder, registry)
        server
            .expect(once(), requestTo("https://api.openai.com/v1/responses"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("초안 입력")))
            .andRespond(withSuccess(response(), MediaType.APPLICATION_JSON))

        val appender = previewLogAppender()
        val result =
            try {
                client.preview("초안 입력: {{context}}", request())
            } finally {
                detach(appender)
            }

        assertThat(result.renderedPrompt).contains("초안 입력").doesNotContain("{{context}}")
        assertThat(result.generatedTurn.question).isEqualTo("SENTINEL_PREVIEW_RESPONSE")
        assertThat(result.generatedTurn.questionTarget).isEqualTo(RetrospectiveItemType.FACT)
        assertThat(result.generatedTurn.inputTokens).isEqualTo(12)
        assertThat(result.generatedTurn.outputTokens).isEqualTo(34)
        assertThat(registry.find("didit.openai.request.duration").tag("operation", "conversation_v2_preview").timer()).isNotNull
        assertThat(appender.events).noneMatch { it.contains("SENTINEL_PREVIEW_RESPONSE") }
        server.verify()
    }

    @Test
    fun `preview - does not log the raw response when the V2 response cannot be parsed`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val registry = SimpleMeterRegistry()
        val client = createClient(builder, registry)
        server
            .expect(once(), requestTo("https://api.openai.com/v1/responses"))
            .andRespond(
                withSuccess(
                    """{"output":[{"content":[{"type":"output_text","text":"SENTINEL_PREVIEW_RESPONSE"}]}]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val appender = previewLogAppender()
        try {
            org.assertj.core.api.Assertions
                .assertThatThrownBy { client.preview("{{context}}", request()) }
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException::class.java)
        } finally {
            detach(appender)
        }

        assertThat(appender.events).noneMatch { it.contains("SENTINEL_PREVIEW_RESPONSE") }
        assertThat(appender.events).noneMatch { it.contains("status: success") }
    }

    private fun createClient(
        builder: RestClient.Builder,
        registry: SimpleMeterRegistry,
    ): AdminPromptPreviewAIClient =
        OpenAiClient(
            restClient = builder.build(),
            objectMapper = jacksonObjectMapper(),
            feedbackPrompts = mockFeedbackPrompts(),
            conversationV2Prompts = ConversationV2Prompts(mockPromptRepository(), jacksonObjectMapper()),
            retrospectiveResultV2Prompts = mockRetrospectiveResultV2Prompts(),
            metrics = OpenAiMetrics(registry),
            apiKey = "test-key",
            model = "test-model",
        )

    private fun request() =
        ConversationTurnAIRequest(
            job = Job.DEVELOPER,
            experience = null,
            messages = emptyList(),
            analysisItems = emptyList(),
            currentMessageId = UUID.randomUUID(),
            consecutiveIrrelevantCount = 0,
        )

    private fun response() =
        """{"output":[{"content":[{"type":"output_text","text":"{\"acknowledgement\":\"알겠습니다.\",\"interpretation\":\"상황을 이해했어요.\",\"question\":\"SENTINEL_PREVIEW_RESPONSE\",\"questionTarget\":\"FACT\",\"relevance\":\"RETROSPECTIVE\",\"analysisUpdates\":[]}"}]}],"usage":{"input_tokens":12,"output_tokens":34}}"""

    private fun previewLogAppender(): CapturedLogAppender {
        val logger = logger()
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        val previousLevel = logger.level
        logger.level = Level.DEBUG
        logger.addAppender(appender)
        return CapturedLogAppender(appender, previousLevel)
    }

    private fun detach(captured: CapturedLogAppender) {
        logger().detachAppender(captured.appender)
        logger().level = captured.previousLevel
        captured.appender.stop()
    }

    private fun logger(): Logger = org.slf4j.LoggerFactory.getLogger(OpenAiClient::class.java) as Logger

    private fun mockFeedbackPrompts() = org.mockito.kotlin.mock<FeedbackPrompts>()

    private fun mockPromptRepository() = org.mockito.kotlin.mock<com.didit.application.prompt.required.PromptRepository>()

    private fun mockRetrospectiveResultV2Prompts() = org.mockito.kotlin.mock<RetrospectiveResultV2Prompts>()

    private data class CapturedLogAppender(
        val appender: ListAppender<ILoggingEvent>,
        val previousLevel: Level?,
    ) {
        val events: List<String>
            get() = appender.list.map { it.formattedMessage }
    }
}
