package com.didit.application.retrospect

import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.ConversationContextMessage
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationTurnAction
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

    @Test
    fun `질문 후보는 충분한 항목과 거부 항목과 직전 질문 대상을 제외한다`() {
        val candidates =
            policy.eligibleQuestionTargets(
                items =
                    listOf(
                        analysisItem(RetrospectiveItemType.FACT, RetrospectiveItemStatus.ENOUGH, "완료"),
                        analysisItem(RetrospectiveItemType.BLOCK, RetrospectiveItemStatus.EMPTY, null, questionAllowed = false),
                        analysisItem(RetrospectiveItemType.PROCESS, RetrospectiveItemStatus.PARTIAL, "진행 중"),
                        analysisItem(RetrospectiveItemType.LEARN, RetrospectiveItemStatus.EMPTY, null),
                    ),
                recentQuestionTargets = listOf(RetrospectiveItemType.PROCESS),
            )

        assertThat(candidates).containsExactly(RetrospectiveItemType.LEARN)
    }

    @Test
    fun `직전 질문 대상이 유일한 후보여도 즉시 반복하지 않는다`() {
        val candidates =
            policy.eligibleQuestionTargets(
                items = listOf(analysisItem(RetrospectiveItemType.PROCESS, RetrospectiveItemStatus.PARTIAL, "진행 중")),
                recentQuestionTargets = listOf(RetrospectiveItemType.PROCESS),
            )

        assertThat(candidates).isEmpty()
    }

    @Test
    fun `준비도가 충족되면 아직 제안하지 않은 회고에 종료 제안을 우선한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.FACT),
                completionRecommended = true,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.OFFER_COMPLETION)
        assertThat(decided.question).isNull()
        assertThat(decided.questionTarget).isNull()
        assertThat(decided.content()).contains("회고를 마칠까요")
    }

    @Test
    fun `질문 턴이 여섯 번 누적되면 완료를 권장한다`() {
        assertThat(policy.shouldRecommendCompletion(readyToComplete = false, completedQuestionCount = 5)).isFalse()
        assertThat(policy.shouldRecommendCompletion(readyToComplete = false, completedQuestionCount = 6)).isTrue()
    }

    @Test
    fun `이미 종료를 제안한 회고에는 같은 제안을 반복하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.PROCESS),
                completionRecommended = true,
                completionPreviouslyOffered = true,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
    }

    @Test
    fun `종료 확인 액션은 모델이 만든 추가 질문을 제거한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE).copy(action = ConversationTurnAction.CONFIRM_COMPLETION),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.FACT),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.CONFIRM_COMPLETION)
        assertThat(decided.question).isNull()
        assertThat(decided.questionTarget).isNull()
    }

    @Test
    fun `허용되지 않은 질문 대상은 질문 없는 응답으로 낮춘다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.LEARN),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.REFLECT)
        assertThat(decided.question).isNull()
        assertThat(decided.questionTarget).isNull()
    }

    @Test
    fun `질문 없는 REFLECT 응답이 비어 있으면 안전한 인정 문구를 채운다`() {
        // Break caught: an empty model REFLECT survives question removal and creates a blank assistant message.
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = " ",
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = emptyList(),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.REFLECT)
        assertThat(decided.content()).isEqualTo("알겠어요.")
        assertThat(decided.question).isNull()
        assertThat(decided.questionTarget).isNull()
    }

    private fun contextMessage(content: String) = ConversationContextMessage(UUID.randomUUID(), Sender.USER, content)

    private fun message(
        sender: Sender = Sender.USER,
        relevance: MessageRelevance?,
    ) = ConversationV2ConversationMessageSnapshot(UUID.randomUUID(), sender, relevance)

    private fun analysisItem(
        status: RetrospectiveItemStatus,
        summary: String?,
    ) = analysisItem(RetrospectiveItemType.FACT, status, summary)

    private fun analysisItem(
        itemType: RetrospectiveItemType,
        status: RetrospectiveItemStatus,
        summary: String?,
        questionAllowed: Boolean = true,
    ) = ConversationV2AnalysisItemSnapshot(itemType, status, summary, questionAllowed)

    private fun update(
        itemType: RetrospectiveItemType,
        status: RetrospectiveItemStatus,
        summary: String,
        evidenceId: UUID,
    ) = ConversationAnalysisUpdate(itemType, status, summary, listOf(evidenceId))

    private fun generated(relevance: MessageRelevance) =
        GeneratedConversationTurn(
            action = ConversationTurnAction.ASK,
            acknowledgement = "확인했어요.",
            interpretation = "",
            question = "다음 질문이에요.",
            questionTarget = RetrospectiveItemType.PROCESS,
            relevance = relevance,
            declinedItemTypes = emptyList(),
            analysisUpdates = emptyList(),
            inputTokens = 0,
            outputTokens = 0,
        )
}
