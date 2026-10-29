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
    fun `FACT와 전체 세 항목과 핵심 성찰 한 항목이 수집되면 완료 준비가 된다`() {
        val ready =
            policy.readyToComplete(
                listOf(
                    analysisItem(RetrospectiveItemType.FACT, RetrospectiveItemStatus.ENOUGH, "면접 공부"),
                    analysisItem(RetrospectiveItemType.PROCESS, RetrospectiveItemStatus.PARTIAL, "퀴즈로 복습"),
                    analysisItem(RetrospectiveItemType.LEARN, RetrospectiveItemStatus.PARTIAL, "멱등성 이해"),
                    analysisItem(RetrospectiveItemType.ACTION, RetrospectiveItemStatus.EMPTY, null),
                ),
            )

        assertThat(ready).isTrue()
    }

    @Test
    fun `전체 세 항목이 있어도 핵심 성찰 항목이 없으면 완료 준비가 아니다`() {
        val ready =
            policy.readyToComplete(
                listOf(
                    analysisItem(RetrospectiveItemType.FACT, RetrospectiveItemStatus.ENOUGH, "면접 공부"),
                    analysisItem(RetrospectiveItemType.FEEL, RetrospectiveItemStatus.PARTIAL, "흥미로움"),
                    analysisItem(RetrospectiveItemType.PROCESS, RetrospectiveItemStatus.PARTIAL, "퀴즈로 복습"),
                    analysisItem(RetrospectiveItemType.LEARN, RetrospectiveItemStatus.EMPTY, null),
                ),
            )

        assertThat(ready).isFalse()
    }

    @Test
    fun `준비도가 충족되면 아직 제안하지 않은 회고에 종료 제안을 우선한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.FACT),
                completionRecommended = true,
                completionPreviouslyOffered = false,
                readyToComplete = true,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.OFFER_COMPLETION)
        assertThat(decided.question).isNull()
        assertThat(decided.questionTarget).isNull()
        assertThat(decided.content()).isEqualTo("지금까지 나눈 내용으로 회고를 마칠 수 있어요.")
    }

    @Test
    fun `준비도가 충족되고 회고 메시지가 두 번이면 완료를 권장한다`() {
        assertThat(
            policy.shouldRecommendCompletion(
                readyToComplete = true,
                retrospectiveMessageCount = 1,
                completedQuestionCount = 2,
            ),
        ).isFalse()
        assertThat(
            policy.shouldRecommendCompletion(
                readyToComplete = true,
                retrospectiveMessageCount = 2,
                completedQuestionCount = 2,
            ),
        ).isTrue()
    }

    @Test
    fun `질문 턴이 다섯 번 누적되면 완료를 권장한다`() {
        assertThat(
            policy.shouldRecommendCompletion(
                readyToComplete = false,
                retrospectiveMessageCount = 1,
                completedQuestionCount = 4,
            ),
        ).isFalse()
        assertThat(
            policy.shouldRecommendCompletion(
                readyToComplete = false,
                retrospectiveMessageCount = 1,
                completedQuestionCount = 5,
            ),
        ).isTrue()
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
    fun `명시적인 종료 의사가 없으면 모델의 종료 확인 액션을 다음 질문으로 보정한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE).copy(action = ConversationTurnAction.CONFIRM_COMPLETION),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.FACT),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.question).isNotBlank()
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.FACT)
    }

    @Test
    fun `허용되지 않은 질문 대상은 다른 질문 후보로 전환한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.LEARN),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.question).isNotBlank()
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.LEARN)
    }

    @Test
    fun `회고 응답에 질문 후보가 남아 있으면 모델의 REFLECT를 구체적인 질문으로 보정한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
        assertThat(decided.question).isNotBlank()
    }

    @Test
    fun `분석 갱신과 거부가 없는 빈 REFLECT는 안전한 다음 질문으로 보정한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "",
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
        assertThat(decided.question).isNotBlank()
    }

    @Test
    fun `질문이 두 번 연속된 뒤 유효한 답변에도 다음 회고 질문을 제공한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.PROCESS),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.question).isNotBlank()
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.PROCESS)
        assertThat(decided.content()).doesNotContain("더 남기고", "더 이어가고", "자유롭게")
    }

    @Test
    fun `사용자가 새 내용을 말하면 다음 회고 항목을 구체적으로 질문한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        acknowledgement = "실제 프로젝트에 적용할 계획까지 세우셨네요.",
                        interpretation = "",
                        question = "진행하면서 어려웠거나 막혔던 점은 무엇이었나요?",
                        questionTarget = RetrospectiveItemType.BLOCK,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.BLOCK),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.question).isNotBlank()
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.BLOCK)
        assertThat(decided.content()).doesNotContain("더 남기고", "더 이어가고", "자유롭게")
    }

    @Test
    fun `해석에 이어라는 표현이 있어도 다음 회고 질문을 유지한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        acknowledgement = "적용 계획까지 세우셨네요.",
                        interpretation = "학습을 실제 업무로 이어가려는 계획이 구체적이에요.",
                        question = "먼저 확인해볼 병목 후보가 있나요?",
                        questionTarget = RetrospectiveItemType.BLOCK,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.BLOCK),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.question).isEqualTo("먼저 확인해볼 병목 후보가 있나요?")
        assertThat(decided.content()).doesNotContain("더 이어가고 싶다면")
    }

    @Test
    fun `해석과 질문에 같은 질문이 있어도 질문은 한 번만 노출한다`() {
        val optionalQuestion = "먼저 확인해볼 병목 후보가 있나요?"
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        acknowledgement = "적용 계획까지 세우셨네요.",
                        interpretation = "구체적인 실행 계획이에요. $optionalQuestion",
                        question = optionalQuestion,
                        questionTarget = RetrospectiveItemType.BLOCK,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.BLOCK),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.content()).doesNotContain("더 이어가고 싶다면")
        assertThat(decided.content().windowed(optionalQuestion.length).count { it == optionalQuestion }).isEqualTo(1)
    }

    @Test
    fun `항목을 거부해도 질문 후보가 남아 있으면 다음 항목을 질문한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        question = null,
                        questionTarget = null,
                        declinedItemTypes = listOf(RetrospectiveItemType.BLOCK),
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
        assertThat(decided.question).isNotBlank()
        assertThat(decided.acknowledgement).isEmpty()
    }

    @Test
    fun `결과 생성이 어려운 상태에서 항목을 세 번 연속 거부해도 전체 종료로 전환하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        declinedItemTypes = listOf(RetrospectiveItemType.BLOCK),
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                readyToComplete = false,
                userMessageContent = "없어",
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
    }

    @Test
    fun `결과 생성이 어려운데 질문 후보가 소진되면 버튼 없는 자유 입력 안내를 반환한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = emptyList(),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                readyToComplete = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.REFLECT)
        assertThat(decided.question).isNull()
        assertThat(decided.questionTarget).isNull()
        assertThat(decided.content())
            .isEqualTo(
                "현재 내용만으로는 회고 결과를 만들기 어려워요. " +
                    "더 남기고 싶은 내용을 이야기하거나, 회고를 끝내려면 상단의 ‘회고 마치기’를 눌러주세요.",
            )
    }

    @Test
    fun `이미 답했다고 하거나 더 이상 없다고 하면 현재 항목을 다시 묻지 않도록 거절로 정규화한다`() {
        listOf("앞에서 다 말했어요", "더 이상 없어요").forEach { content ->
            val normalized =
                policy.normalizeItemDecline(
                    generated = generated(MessageRelevance.RETROSPECTIVE),
                    lastQuestionTarget = RetrospectiveItemType.FEEL,
                    userMessageContent = content,
                )

            assertThat(normalized.declinedItemTypes).describedAs(content).containsExactly(RetrospectiveItemType.FEEL)
        }
    }

    @Test
    fun `사용자의 없음 응답만으로는 종료 확인으로 전환하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "알겠어요.",
                        interpretation = "더 남기고 싶은 내용이 있다면 이어서 말씀해주세요.",
                        question = null,
                        questionTarget = null,
                        declinedItemTypes = emptyList(),
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                userMessageContent = "없는 것 같아.",
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
        assertThat(decided.question).isNotBlank()
    }

    @Test
    fun `결과를 만들기 어려울 때 전체 종료 의사를 밝히면 결과 없는 종료 안내를 반환한다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                readyToComplete = false,
                userMessageContent = "더 할 말 없어",
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.CONFIRM_EXIT)
        assertThat(decided.questionTarget).isNull()
        assertThat(decided.question).isNull()
        assertThat(decided.content())
            .isEqualTo(
                "현재 내용만으로는 회고 결과를 만들기 어려워요. " +
                    "더 남기고 싶은 내용을 이야기하거나, 회고를 끝내려면 상단의 ‘회고 마치기’를 눌러주세요.",
            )
    }

    @Test
    fun `결과를 만들 수 있을 때 전체 종료 의사를 밝히면 말풍선 없이 종료 확인을 반환한다`() {
        listOf(
            "그만할게",
            "끝내자",
            "끝내자고",
            "끝낼게",
            "회고 마칠게",
            "마칠래요",
            "오늘 회고는 여기까지 할게요",
            "종료할게",
        ).forEach { content ->
            val decided =
                policy.decideTurn(
                    generated = generated(MessageRelevance.RETROSPECTIVE),
                    eligibleQuestionTargets = emptyList(),
                    completionRecommended = false,
                    completionPreviouslyOffered = false,
                    readyToComplete = true,
                    userMessageContent = content,
                )

            assertThat(decided.action).describedAs(content).isEqualTo(ConversationTurnAction.CONFIRM_COMPLETION)
            assertThat(decided.content()).describedAs(content).isEmpty()
        }
    }

    @Test
    fun `업무를 끝내려는 다음 행동은 회고 종료 의사로 해석하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated = generated(MessageRelevance.RETROSPECTIVE),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                userMessageContent = "이번 주 안에 배포 작업을 끝내고 싶어요",
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
    }

    @Test
    fun `사용자 응답에는 내부 회고 항목과 수집 상태를 노출하지 않는다`() {
        // Break caught: model wording such as BLOCK/ACTION or an empty analysis item reaches the user unchanged.
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "아직 진행 전이라 막힌 점은 없었던 거네요.",
                        interpretation = "회고상 BLOCK은 비어 있고, ACTION만 계획 수준으로 남아 있어요.",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.LEARN),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.content())
            .contains("아직 진행 전이라 막힌 점은 없었던 거네요.")
            .doesNotContain("BLOCK", "ACTION", "회고상", "비어")
    }

    @Test
    fun `업무 맥락에서 사용한 enum과 같은 용어는 사용자 응답에 유지한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "FACT 테이블 상태와 인덱스를 점검하셨네요.",
                        interpretation = "조회 성능을 확인한 과정이 구체적이에요.",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = emptyList(),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                userMessageContent = "FACT 테이블 상태와 인덱스를 점검했어요.",
                readyToComplete = true,
            )

        assertThat(decided.content()).contains("FACT 테이블 상태")
    }

    @Test
    fun `사용자가 말하지 않은 내부 enum 이름은 단독으로도 노출하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "ACTION은 아직 정하지 않았어요.",
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = emptyList(),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                userMessageContent = "다음 계획은 아직 없어요.",
            )

        assertThat(decided.content()).doesNotContain("ACTION").isNotBlank()
    }

    @Test
    fun `내부 요청 필드명은 사용자 응답에 노출하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "completionRecommended가 아직 false예요.",
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = emptyList(),
                completionRecommended = false,
                completionPreviouslyOffered = false,
                userMessageContent = "조금 더 이야기할게요.",
            )

        assertThat(decided.content()).doesNotContain("completionRecommended").isNotBlank()
    }

    @Test
    fun `내부 회고 상태가 포함된 질문은 안전한 사용자 질문으로 교체한다`() {
        // Break caught: an internal analysis label survives inside a user-facing ASK question.
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        question = "비어 있는 LEARN 항목을 채우려면 무엇을 배웠나요?",
                        questionTarget = RetrospectiveItemType.LEARN,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.LEARN),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.question).isEqualTo("이번 경험을 통해 새롭게 알게 된 점은 무엇인가요?")
        assertThat(decided.content()).doesNotContain("LEARN", "비어")
    }

    @Test
    fun `회고 연결 응답에서 항목을 거부해도 종료 확인으로 전환하지 않는다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.BRIDGEABLE).copy(
                        action = ConversationTurnAction.CONFIRM_COMPLETION,
                        question = "다음에는 무엇을 다르게 해보고 싶나요?",
                        questionTarget = RetrospectiveItemType.ACTION,
                        declinedItemTypes = listOf(RetrospectiveItemType.BLOCK),
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
        assertThat(decided.question).isNotBlank()
    }

    @Test
    fun `이어 말하기로 분류된 REFLECT는 새 질문으로 덮어쓰지 않는다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.BRIDGEABLE).copy(
                        action = ConversationTurnAction.REFLECT,
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = false,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.REFLECT)
        assertThat(decided.question).isNull()
    }

    @Test
    fun `계속하기를 선택했지만 질문 후보가 없으면 자유 입력 안내로 돌아간다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.REFLECT,
                        acknowledgement = "좋아요.",
                        interpretation = "",
                        question = null,
                        questionTarget = null,
                    ),
                eligibleQuestionTargets = emptyList(),
                completionRecommended = false,
                completionPreviouslyOffered = true,
                continuationRequested = true,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.REFLECT)
        assertThat(decided.content())
            .isEqualTo(
                "현재 내용만으로는 회고 결과를 만들기 어려워요. " +
                    "더 남기고 싶은 내용을 이야기하거나, 회고를 끝내려면 상단의 ‘회고 마치기’를 눌러주세요.",
            )
    }

    @Test
    fun `계속하기 의도는 모델의 종료 확인 응답보다 우선한다`() {
        val decided =
            policy.decideTurn(
                generated =
                    generated(MessageRelevance.RETROSPECTIVE).copy(
                        action = ConversationTurnAction.CONFIRM_COMPLETION,
                        acknowledgement = "회고를 마무리할까요?",
                        interpretation = "종료할 준비가 된 것 같아요.",
                        question = "정말 회고를 마칠까요?",
                        questionTarget = RetrospectiveItemType.ACTION,
                    ),
                eligibleQuestionTargets = listOf(RetrospectiveItemType.ACTION),
                completionRecommended = false,
                completionPreviouslyOffered = true,
                continuationRequested = true,
            )

        assertThat(decided.action).isEqualTo(ConversationTurnAction.ASK)
        assertThat(decided.questionTarget).isEqualTo(RetrospectiveItemType.ACTION)
        assertThat(decided.content()).doesNotContain("마무리", "마칠", "종료")
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
                readyToComplete = true,
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
        content: String = "",
    ) = ConversationV2ConversationMessageSnapshot(UUID.randomUUID(), sender, relevance, content)

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
