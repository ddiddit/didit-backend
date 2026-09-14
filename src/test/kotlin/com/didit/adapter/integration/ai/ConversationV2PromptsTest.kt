package com.didit.adapter.integration.ai

import com.didit.application.prompt.required.PromptRepository
import com.didit.application.retrospect.required.ConversationAnalysisItem
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.domain.auth.UserExperience
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import com.didit.domain.shared.Job
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.UUID

class ConversationV2PromptsTest {
    private val prompts = ConversationV2Prompts(mock<PromptRepository>(), jacksonObjectMapper())

    @Test
    fun `render - replaces context once with the complete conversation turn request`() {
        val currentMessageId = UUID.fromString("00000000-0000-0000-0000-000000000007")
        val request =
            ConversationTurnAIRequest(
                job = Job.DEVELOPER,
                experience = UserExperience.YEARS_3_TO_5,
                messages =
                    listOf(
                        ConversationContextMessage(
                            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            sender = Sender.USER,
                            content = "배포 오류를 해결했어요.",
                        ),
                    ),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        ConversationAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                currentMessageId = currentMessageId,
                consecutiveIrrelevantCount = 2,
            )

        val rendered = prompts.render("입력: {{context}}", request)

        assertThat(rendered)
            .contains("\"job\":\"DEVELOPER\"")
            .contains("\"experience\":\"YEARS_3_TO_5\"")
            .contains("배포 오류를 해결했어요.")
            .contains(currentMessageId.toString())
            .contains("\"analysisItems\"")
            .doesNotContain("{{context}}")
    }

    @Test
    fun `render - leaves a draft without a context placeholder unchanged`() {
        val draft = "context placeholder가 없는 초안"

        val rendered = prompts.render(draft, request())

        assertThat(rendered).isEqualTo(draft)
    }

    private fun request() =
        ConversationTurnAIRequest(
            job = null,
            experience = null,
            messages = emptyList(),
            analysisItems = emptyList(),
            currentMessageId = UUID.randomUUID(),
            consecutiveIrrelevantCount = 0,
        )
}
