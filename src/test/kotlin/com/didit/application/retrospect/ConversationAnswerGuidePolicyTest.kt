package com.didit.application.retrospect

import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.retrospect.ConversationAnswerGuideMode
import com.didit.domain.retrospect.ConversationTurnAction
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConversationAnswerGuidePolicyTest {
    private val policy = ConversationAnswerGuidePolicy(AttachmentSensitiveDataDetector())

    @Test
    fun `FACT 맥락이 있는 BLOCK 첫 질문은 필요할 때 펼치는 예시를 제공한다`() {
        val guide =
            policy.decide(
                generated = generated(RetrospectiveItemType.BLOCK, listOf("테스트 범위가 예상보다 넓었어요", "환경 설정에서 시간이 걸렸어요")),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
            )

        assertThat(guide?.mode).isEqualTo(ConversationAnswerGuideMode.ON_DEMAND)
        assertThat(guide?.examples).containsExactly("테스트 범위가 예상보다 넓었어요", "환경 설정에서 시간이 걸렸어요")
    }

    @Test
    fun `ACTION 첫 질문은 맥락과 유지 선택이 있으면 바로 보이는 예시를 제공한다`() {
        val guide =
            policy.decide(
                generated =
                    generated(
                        RetrospectiveItemType.ACTION,
                        listOf("면접 답변으로 소리 내어 연습해볼래요", "지금처럼 퀴즈로 복습하는 방식을 유지할래요"),
                    ),
                decided = generated(RetrospectiveItemType.ACTION),
                items =
                    items(
                        fact = RetrospectiveItemStatus.PARTIAL,
                        process = RetrospectiveItemStatus.PARTIAL,
                    ),
                sensitiveDataDetected = false,
            )

        assertThat(guide?.mode).isEqualTo(ConversationAnswerGuideMode.INLINE)
        assertThat(guide?.examples).hasSize(2)
    }

    @Test
    fun `지원하지 않는 target 또는 ASK가 아닌 응답에는 예시를 제공하지 않는다`() {
        val unsupported =
            policy.decide(
                generated = generated(RetrospectiveItemType.PROCESS, listOf("예시 하나", "예시 둘")),
                decided = generated(RetrospectiveItemType.PROCESS),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
            )
        val reflect =
            policy.decide(
                generated = generated(RetrospectiveItemType.BLOCK, listOf("예시 하나", "예시 둘")),
                decided =
                    generated(RetrospectiveItemType.BLOCK).copy(
                        action = ConversationTurnAction.REFLECT,
                        question = null,
                        questionTarget = null,
                    ),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
            )

        assertThat(unsupported).isNull()
        assertThat(reflect).isNull()
    }

    @Test
    fun `이미 일부 수집된 target이나 맥락이 부족한 첫 질문에는 예시를 제공하지 않는다`() {
        val partialTarget =
            policy.decide(
                generated = generated(RetrospectiveItemType.BLOCK, listOf("예시 하나", "예시 둘")),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL, block = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
            )
        val missingContext =
            policy.decide(
                generated = generated(RetrospectiveItemType.LEARN, listOf("예시 하나", "예시 둘")),
                decided = generated(RetrospectiveItemType.LEARN),
                items = items(),
                sensitiveDataDetected = false,
            )

        assertThat(partialTarget).isNull()
        assertThat(missingContext).isNull()
    }

    @Test
    fun `서버가 질문 target을 보정하면 모델이 만든 다른 target 예시는 버린다`() {
        val guide =
            policy.decide(
                generated = generated(RetrospectiveItemType.BLOCK, listOf("예시 하나", "예시 둘")),
                decided = generated(RetrospectiveItemType.LEARN),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
            )

        assertThat(guide).isNull()
    }

    @Test
    fun `과거에 한 번 질문했던 target에는 비어 있어도 예시를 다시 제공하지 않는다`() {
        val guide =
            policy.decide(
                generated = generated(RetrospectiveItemType.BLOCK, listOf("예시 하나", "예시 둘")),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
                previouslyAskedTargets = setOf(RetrospectiveItemType.BLOCK),
            )

        assertThat(guide).isNull()
    }

    @Test
    fun `공백 중복 길이 위반 또는 두 개 미만 예시는 전체를 버린다`() {
        val invalidExamples =
            listOf(
                listOf("예시 하나", " "),
                listOf("같은 예시", "같은 예시"),
                listOf("가".repeat(61), "예시 둘"),
                listOf("예시 하나"),
            )

        invalidExamples.forEach { examples ->
            val guide =
                policy.decide(
                    generated = generated(RetrospectiveItemType.BLOCK, examples),
                    decided = generated(RetrospectiveItemType.BLOCK),
                    items = items(fact = RetrospectiveItemStatus.PARTIAL),
                    sensitiveDataDetected = false,
                )

            assertThat(guide).isNull()
        }
    }

    @Test
    fun `민감정보를 마스킹한 뒤에도 유효한 예시는 안전하게 제공한다`() {
        val guide =
            policy.decide(
                generated =
                    generated(
                        RetrospectiveItemType.BLOCK,
                        listOf("worker@example.com에게 확인이 필요했어요", "테스트 환경 접근이 어려웠어요"),
                    ),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = true,
            )

        assertThat(guide?.examples).containsExactly("[민감정보]에게 확인이 필요했어요", "테스트 환경 접근이 어려웠어요")
    }

    @Test
    fun `내부 회고 항목이나 상태가 포함된 예시는 전체를 버린다`() {
        val guide =
            policy.decide(
                generated =
                    generated(
                        RetrospectiveItemType.BLOCK,
                        listOf("BLOCK은 아직 확인되지 않았어요", "테스트 범위가 예상보다 넓었어요"),
                    ),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
                userMessageContent = "테스트 범위가 예상보다 넓었어요.",
            )

        assertThat(guide).isNull()
    }

    @Test
    fun `업무 맥락에서 사용한 enum과 같은 용어는 답변 예시에 유지한다`() {
        val guide =
            policy.decide(
                generated =
                    generated(
                        RetrospectiveItemType.BLOCK,
                        listOf("FACT 테이블 상태를 확인했어요", "복합 인덱스를 검토했어요"),
                    ),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
                userMessageContent = "FACT 테이블 상태를 확인했어요.",
            )

        assertThat(guide?.examples).containsExactly("FACT 테이블 상태를 확인했어요", "복합 인덱스를 검토했어요")
    }

    @Test
    fun `내부 응답 필드명이 포함된 예시는 전체를 버린다`() {
        val guide =
            policy.decide(
                generated =
                    generated(
                        RetrospectiveItemType.BLOCK,
                        listOf("analysisUpdates가 비어 있어요", "테스트 범위가 예상보다 넓었어요"),
                    ),
                decided = generated(RetrospectiveItemType.BLOCK),
                items = items(fact = RetrospectiveItemStatus.PARTIAL),
                sensitiveDataDetected = false,
                userMessageContent = "테스트 범위가 예상보다 넓었어요.",
            )

        assertThat(guide).isNull()
    }

    private fun generated(
        target: RetrospectiveItemType,
        examples: List<String> = emptyList(),
    ) = GeneratedConversationTurn(
        action = ConversationTurnAction.ASK,
        acknowledgement = "좋아요.",
        interpretation = "",
        question = "질문입니다.",
        questionTarget = target,
        relevance = MessageRelevance.RETROSPECTIVE,
        analysisUpdates = emptyList(),
        answerExamples = examples,
        inputTokens = 0,
        outputTokens = 0,
    )

    private fun items(
        fact: RetrospectiveItemStatus = RetrospectiveItemStatus.EMPTY,
        process: RetrospectiveItemStatus = RetrospectiveItemStatus.EMPTY,
        block: RetrospectiveItemStatus = RetrospectiveItemStatus.EMPTY,
    ): List<ConversationV2AnalysisItemSnapshot> =
        RetrospectiveItemType.entries.map { type ->
            ConversationV2AnalysisItemSnapshot(
                itemType = type,
                status =
                    when (type) {
                        RetrospectiveItemType.FACT -> fact
                        RetrospectiveItemType.PROCESS -> process
                        RetrospectiveItemType.BLOCK -> block
                        else -> RetrospectiveItemStatus.EMPTY
                    },
                summary = null,
            )
        }
}
