package com.didit.application.retrospect

import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ConversationV2TurnPolicyTest {
    private val policy = ConversationV2TurnPolicy()

    @Test
    fun `최근 context만 남겨 오래된 메시지가 문자 한도를 넘으면 제외한다`() {
        // Break caught: context selection changes from newest-first to oldest-first.
        val older = contextMessage("가".repeat(20_000))
        val newer = contextMessage("나".repeat(20_000))

        val context = policy.trimContext(listOf(older, newer), maxCharacters = 30_000)

        assertThat(context).containsExactly(newer)
    }

    @Test
    fun `현재 메시지 앞의 연속된 OFF_TOPIC과 SERVICE_HELP 사용자 메시지만 무관 횟수에 포함한다`() {
        // Break caught: OFF_TOPIC/SERVICE_HELP turns are not counted or a retrospective turn fails to reset the count.
        val retrospective = message(relevance = MessageRelevance.RETROSPECTIVE)
        val offTopic = message(relevance = MessageRelevance.OFF_TOPIC)
        val serviceHelp = message(relevance = MessageRelevance.SERVICE_HELP)
        val current = message(relevance = null)

        val count =
            policy.consecutiveIrrelevantCount(
                listOf(retrospective, offTopic, serviceHelp, current),
                current.id,
            )

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `RETROSPECTIVE 사용자 메시지만 analysis evidence로 인정한다`() {
        // Break caught: AI, unclassified, OFF_TOPIC, or SERVICE_HELP messages become evidence.
        val accepted = message(relevance = MessageRelevance.RETROSPECTIVE)
        val ai = message(sender = Sender.AI, relevance = MessageRelevance.RETROSPECTIVE)
        val unclassified = message(relevance = null)
        val offTopic = message(relevance = MessageRelevance.OFF_TOPIC)
        val serviceHelp = message(relevance = MessageRelevance.SERVICE_HELP)

        val evidence =
            policy.evidenceDecision(
                listOf(accepted, ai, unclassified, offTopic, serviceHelp),
                listOf(ai.id, unclassified.id, accepted.id, offTopic.id, serviceHelp.id),
            )

        assertThat(evidence.acceptedMessageIds).containsExactly(accepted.id)
        assertThat(evidence.rejectedMessageIds).containsExactly(ai.id, unclassified.id, offTopic.id, serviceHelp.id)
    }

    @Test
    fun `낮은 status update도 summary는 갱신하지만 기존 status는 보존한다`() {
        // Break caught: a lower model status regresses an item or prevents its new summary from being displayed.
        val item = analysisItem(RetrospectiveItemStatus.ENOUGH, "기존 요약")
        val evidence = message(relevance = MessageRelevance.RETROSPECTIVE)
        val update = update(item.itemType, RetrospectiveItemStatus.PARTIAL, "새 요약", evidence.id)

        val decision = policy.applyAnalysisUpdates(listOf(item), listOf(update), listOf(evidence)).single()

        assertThat(decision.after)
            .isEqualTo(ConversationV2AnalysisItemSnapshot(item.itemType, RetrospectiveItemStatus.ENOUGH, "새 요약"))
    }

    @Test
    fun `같은 항목 update는 모델 배열 순서대로 직전 decision 결과에 적용한다`() {
        // Break caught: updates are deduplicated/reordered instead of preserving the model array order.
        val item = analysisItem(RetrospectiveItemStatus.EMPTY, null)
        val evidence = message(relevance = MessageRelevance.RETROSPECTIVE)
        val first = update(item.itemType, RetrospectiveItemStatus.PARTIAL, "첫 요약", evidence.id)
        val second = update(item.itemType, RetrospectiveItemStatus.ENOUGH, "둘째 요약", evidence.id)

        val decisions = policy.applyAnalysisUpdates(listOf(item), listOf(first, second), listOf(evidence))

        assertThat(decisions.map { it.after })
            .containsExactly(
                ConversationV2AnalysisItemSnapshot(item.itemType, RetrospectiveItemStatus.PARTIAL, "첫 요약"),
                ConversationV2AnalysisItemSnapshot(item.itemType, RetrospectiveItemStatus.ENOUGH, "둘째 요약"),
            )
    }

    @Test
    fun `초기 안내와 generated turn snapshot은 운영 표시 규칙을 보존한다`() {
        // Break caught: preview/production diverge on intro content or OFF_TOPIC/SERVICE_HELP assistant message type.
        val intro = policy.initialIntro()

        assertThat(intro).isEqualTo(
            ConversationV2MessageSnapshot(
                content = "오늘 어떤 일을 하셨나요?",
                messageType = ConversationMessageType.INTRO,
                supportingContent = "오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.",
            ),
        )
        MessageRelevance.entries.forEach { relevance ->
            val assistant = policy.assistantSnapshot(generated(relevance))
            assertThat(assistant.content).isEqualTo("확인했어요.\n다음 질문이에요.")
            assertThat(assistant.messageType).isEqualTo(
                if (relevance == MessageRelevance.OFF_TOPIC || relevance == MessageRelevance.SERVICE_HELP) {
                    ConversationMessageType.SYSTEM_GUIDE
                } else {
                    ConversationMessageType.CONVERSATION
                },
            )
        }
    }

    private fun contextMessage(content: String) = ConversationContextMessage(UUID.randomUUID(), Sender.USER, content)

    private fun message(
        sender: Sender = Sender.USER,
        relevance: MessageRelevance?,
    ) = ConversationV2ConversationMessageSnapshot(UUID.randomUUID(), sender, relevance)

    private fun analysisItem(
        status: RetrospectiveItemStatus,
        summary: String?,
    ) = ConversationV2AnalysisItemSnapshot(RetrospectiveItemType.FACT, status, summary)

    private fun update(
        itemType: RetrospectiveItemType,
        status: RetrospectiveItemStatus,
        summary: String,
        evidenceId: UUID,
    ) = ConversationAnalysisUpdate(itemType, status, summary, listOf(evidenceId))

    private fun generated(relevance: MessageRelevance) =
        GeneratedConversationTurn(
            acknowledgement = "확인했어요.",
            interpretation = "",
            question = "다음 질문이에요.",
            questionTarget = null,
            relevance = relevance,
            analysisUpdates = emptyList(),
            inputTokens = 0,
            outputTokens = 0,
        )
}
