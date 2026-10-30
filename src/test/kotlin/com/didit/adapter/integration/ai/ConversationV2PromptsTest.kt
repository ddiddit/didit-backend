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
                eligibleQuestionTargets = listOf(RetrospectiveItemType.FACT, RetrospectiveItemType.LEARN),
                recentQuestionTargets = listOf(RetrospectiveItemType.PROCESS),
                completionRecommended = true,
                completionPreviouslyOffered = false,
                continuationRequested = true,
            )

        val rendered = prompts.render("입력: {{context}}", request)

        assertThat(rendered)
            .contains("\"job\":\"DEVELOPER\"")
            .contains("\"experience\":\"YEARS_3_TO_5\"")
            .contains("배포 오류를 해결했어요.")
            .contains(currentMessageId.toString())
            .contains("\"analysisItems\"")
            .contains("\"eligibleQuestionTargets\":[\"FACT\",\"LEARN\"]")
            .contains("\"recentQuestionTargets\":[\"PROCESS\"]")
            .contains("\"completionRecommended\":true")
            .contains("\"continuationRequested\":true")
            .doesNotContain("{{context}}")
    }

    @Test
    fun `render - leaves a draft without a context placeholder unchanged`() {
        val draft = "context placeholder가 없는 초안"

        val rendered = prompts.render(draft, request())

        assertThat(rendered).isEqualTo(draft)
    }

    @Test
    fun `build - default prompt requires REFLECT to include visible content`() {
        // Break caught: the default prompt permits a question-free REFLECT with no visible assistant content.
        val rendered = prompts.build(request())

        assertThat(rendered).contains("REFLECT일 때는 acknowledgement와 interpretation 중 하나 이상을 반드시 작성하세요.")
    }

    @Test
    fun `build - default prompt only collects explicit learning and next action`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("LEARN은 사용자가 직접 말한 배움")
            .contains("ACTION은 사용자가 직접 말한 다음 행동")
    }

    @Test
    fun `build - default prompt continues with another item after completion is declined`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("continuationRequested가 true이면")
            .contains("질문 가능한 다른 항목")
            .contains("'모르겠어' 또는 '생각나지 않아'")
            .contains("자유 입력을 유도하지 말고")
    }

    @Test
    fun `build - default prompt distinguishes current question refusal from whole retrospective completion`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("'없어', '없는 것 같아', '딱히 없어'")
            .contains("회고 전체를 마치려는 의사로 해석하지 마세요")
            .contains("질문 가능한 다른 항목")
            .contains("'더 할 말 없어', '그만할게', '끝내자', '회고 마칠게'")
            .contains("CONFIRM_COMPLETION")
            .contains("declinedItemTypes")
            .doesNotContain("해당 항목을 declinedItemTypes에 넣고 CONFIRM_COMPLETION")
    }

    @Test
    fun `build - default prompt treats empty items as candidates instead of mandatory questions`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("현재 이야기와 자연스럽게 이어지는 질문")
            .contains("빈 항목을 채우기 위해 질문하지 마세요")
            .doesNotContain("REFLECT보다 ASK를 우선하세요")
    }

    @Test
    fun `build - default prompt connects a changed question topic naturally`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("질문 주제가 바뀌면")
            .contains("'그렇다면', '그럼'")
            .contains("같은 주제의 후속 질문에는")
    }

    @Test
    fun `build - default prompt generates bounded contextual answer examples in the same response`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("answerExamples")
            .contains("2개에서 3개")
            .contains("별도 질문이나 추가 호출 없이")
            .contains("이번 방식을 유지")
    }

    @Test
    fun `build - default prompt hides internal analysis format from the user`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("내부 항목명")
            .contains("수집 상태")
            .contains("사용자에게 노출하지 마세요")
    }

    @Test
    fun `build - default prompt forbids generic free input guidance`() {
        val rendered = prompts.build(request())

        assertThat(rendered)
            .contains("'더 남기고 싶은 내용', '자유롭게 이어서'")
            .contains("자유 입력 안내를 작성하지 마세요")
            .contains("짧거나 단순한 답변에는 빈 문자열")
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
