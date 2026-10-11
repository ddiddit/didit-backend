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
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ConversationV2TurnPolicy {
    companion object {
        private const val MIN_RETROSPECTIVE_MESSAGES_FOR_READY_COMPLETION = 2
        private const val MAX_QUESTION_TURNS = 5
        private const val DEFAULT_REFLECT_ACKNOWLEDGEMENT = "알겠어요."
        private const val FREE_INPUT_GUIDE = "더 남기고 싶은 내용이 있다면 자유롭게 이어서 말씀해주세요."
        private val CONTINUATION_GUIDE_PHRASES =
            listOf(
                "더 말하고 싶",
                "더 이어가고 싶",
                "자유롭게 말씀",
                "자유롭게 이어서",
                "편하게 말씀",
            )
    }

    fun initialIntro(): ConversationV2MessageSnapshot =
        ConversationV2MessageSnapshot(
            content = "오늘 어떤 일을 하셨나요?",
            messageType = ConversationMessageType.INTRO,
            supportingContent = "오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.",
        )

    fun trimContext(
        messages: List<ConversationContextMessage>,
        maxCharacters: Int,
    ): List<ConversationContextMessage> {
        var used = 0
        return messages
            .asReversed()
            .takeWhile {
                used += it.content.length
                used <= maxCharacters
            }.asReversed()
    }

    fun consecutiveIrrelevantCount(
        messages: List<ConversationV2ConversationMessageSnapshot>,
        currentMessageId: UUID,
    ): Int =
        messages
            .filter { it.sender == Sender.USER && it.id != currentMessageId }
            .asReversed()
            .takeWhile { it.relevance != MessageRelevance.RETROSPECTIVE }
            .count { it.relevance != null }

    fun evidenceDecision(
        messages: List<ConversationV2ConversationMessageSnapshot>,
        requestedMessageIds: List<UUID>,
    ): ConversationV2EvidenceDecision {
        val validIds =
            messages
                .filter { it.sender == Sender.USER && it.relevance == MessageRelevance.RETROSPECTIVE }
                .map { it.id }
                .toSet()
        val acceptedMessageIds = requestedMessageIds.filter { it in validIds }
        return ConversationV2EvidenceDecision(
            acceptedMessageIds = acceptedMessageIds,
            rejectedMessageIds = requestedMessageIds.filterNot { it in validIds },
        )
    }

    fun assistantSnapshot(generated: GeneratedConversationTurn): ConversationV2MessageSnapshot =
        ConversationV2MessageSnapshot(
            content = generated.content(),
            messageType =
                if (generated.relevance == MessageRelevance.OFF_TOPIC || generated.relevance == MessageRelevance.SERVICE_HELP) {
                    ConversationMessageType.SYSTEM_GUIDE
                } else {
                    ConversationMessageType.CONVERSATION
                },
        )

    fun eligibleQuestionTargets(
        items: List<ConversationV2AnalysisItemSnapshot>,
        recentQuestionTargets: List<RetrospectiveItemType>,
    ): List<RetrospectiveItemType> {
        val candidates =
            items
                .filter { it.questionAllowed && it.status != RetrospectiveItemStatus.ENOUGH }
                .map { it.itemType }
        val lastQuestionTarget = recentQuestionTargets.lastOrNull()
        return candidates.filterNot { it == lastQuestionTarget }
    }

    fun shouldRecommendCompletion(
        readyToComplete: Boolean,
        completedQuestionCount: Int,
        retrospectiveMessageCount: Int = 0,
    ): Boolean =
        (readyToComplete && retrospectiveMessageCount >= MIN_RETROSPECTIVE_MESSAGES_FOR_READY_COMPLETION) ||
            completedQuestionCount >= MAX_QUESTION_TURNS

    fun readyToComplete(items: List<ConversationV2AnalysisItemSnapshot>): Boolean {
        val statuses = items.associate { it.itemType to it.status }

        fun collected(type: RetrospectiveItemType) = statuses[type] != null && statuses[type] != RetrospectiveItemStatus.EMPTY
        return collected(RetrospectiveItemType.FACT) &&
            statuses.values.count { it != RetrospectiveItemStatus.EMPTY } >= 3 &&
            listOf(
                RetrospectiveItemType.STRENGTH,
                RetrospectiveItemType.BLOCK,
                RetrospectiveItemType.LEARN,
                RetrospectiveItemType.ACTION,
            ).any(::collected)
    }

    fun decideTurn(
        generated: GeneratedConversationTurn,
        eligibleQuestionTargets: List<RetrospectiveItemType>,
        completionRecommended: Boolean,
        completionPreviouslyOffered: Boolean,
        continuationRequested: Boolean = false,
        hasMeaningfulAnalysisUpdate: Boolean = false,
        completedQuestionCount: Int = 0,
        consecutiveAskCount: Int = 0,
        userMessageContent: String? = null,
    ): GeneratedConversationTurn =
        decideSanitizedTurn(
            generated = generated.sanitizeUserFacingContent(userMessageContent),
            eligibleQuestionTargets = eligibleQuestionTargets,
            completionRecommended = completionRecommended,
            completionPreviouslyOffered = completionPreviouslyOffered,
            continuationRequested = continuationRequested,
            hasMeaningfulAnalysisUpdate = hasMeaningfulAnalysisUpdate,
            completedQuestionCount = completedQuestionCount,
            consecutiveAskCount = consecutiveAskCount,
        ).ensureVisibleContent()

    private fun decideSanitizedTurn(
        generated: GeneratedConversationTurn,
        eligibleQuestionTargets: List<RetrospectiveItemType>,
        completionRecommended: Boolean,
        completionPreviouslyOffered: Boolean,
        continuationRequested: Boolean,
        hasMeaningfulAnalysisUpdate: Boolean,
        completedQuestionCount: Int,
        consecutiveAskCount: Int,
    ): GeneratedConversationTurn {
        if (continuationRequested) {
            val target =
                generated.questionTarget
                    ?.takeIf { generated.action == ConversationTurnAction.ASK && !generated.question.isNullOrBlank() }
                    ?.takeIf { it in eligibleQuestionTargets }
                    ?: eligibleQuestionTargets.firstOrNull()
            return if (target == null) {
                generated.copy(
                    action = ConversationTurnAction.REFLECT,
                    acknowledgement = "좋아요. 더 남기고 싶은 내용을 이어서 말씀해주세요.",
                    interpretation = "",
                    question = null,
                    questionTarget = null,
                )
            } else {
                generated.copy(
                    action = ConversationTurnAction.ASK,
                    acknowledgement = "좋아요.",
                    interpretation = "",
                    question =
                        generated.question?.takeIf {
                            generated.action == ConversationTurnAction.ASK &&
                                generated.questionTarget == target &&
                                it.isNotBlank()
                        } ?: fallbackQuestion(target),
                    questionTarget = target,
                )
            }
        }
        if (generated.action == ConversationTurnAction.CONFIRM_COMPLETION) {
            return generated.copy(question = null, questionTarget = null)
        }
        if (
            generated.relevance == MessageRelevance.RETROSPECTIVE &&
            completionRecommended &&
            !completionPreviouslyOffered
        ) {
            return generated.copy(
                action = ConversationTurnAction.OFFER_COMPLETION,
                interpretation = "충분히 돌아본 것 같아요. 지금까지의 내용으로 회고를 마칠까요?",
                question = null,
                questionTarget = null,
            )
        }
        if (generated.declinedItemTypes.isNotEmpty()) {
            return generated.asReflect(freeInputGuide = true)
        }
        if (
            generated.relevance == MessageRelevance.RETROSPECTIVE &&
            hasMeaningfulAnalysisUpdate &&
            completedQuestionCount > 0 &&
            consecutiveAskCount == 0
        ) {
            return generated.asReflect(freeInputGuide = true, optionalContinuationQuestion = generated.question)
        }
        if (
            generated.relevance == MessageRelevance.RETROSPECTIVE &&
            hasMeaningfulAnalysisUpdate &&
            consecutiveAskCount >= 2
        ) {
            return generated.asReflect(freeInputGuide = true, optionalContinuationQuestion = generated.question)
        }
        if (
            generated.relevance == MessageRelevance.RETROSPECTIVE &&
            hasMeaningfulAnalysisUpdate &&
            generated.action == ConversationTurnAction.REFLECT
        ) {
            return generated.asReflect(freeInputGuide = true)
        }
        val fallbackTarget = eligibleQuestionTargets.firstOrNull()
        val shouldContinueWithQuestion =
            fallbackTarget != null &&
                generated.relevance == MessageRelevance.RETROSPECTIVE
        if (
            generated.action != ConversationTurnAction.ASK ||
            generated.question.isNullOrBlank() ||
            generated.questionTarget !in eligibleQuestionTargets
        ) {
            if (shouldContinueWithQuestion) {
                return generated.copy(
                    action = ConversationTurnAction.ASK,
                    question = fallbackQuestion(checkNotNull(fallbackTarget)),
                    questionTarget = fallbackTarget,
                )
            }
            return generated.asReflect()
        }
        return generated
    }

    private fun GeneratedConversationTurn.asReflect(
        freeInputGuide: Boolean = false,
        optionalContinuationQuestion: String? = null,
    ): GeneratedConversationTurn {
        val baseInterpretation =
            optionalContinuationQuestion
                ?.takeIf(String::isNotBlank)
                ?.let { interpretation.replace(it, "").trim() }
                ?: interpretation
        val reflectInterpretation =
            if (freeInputGuide) {
                when {
                    interpretation.hasContinuationGuide() -> interpretation
                    !optionalContinuationQuestion.isNullOrBlank() ->
                        listOf(
                            baseInterpretation,
                            "더 이어가고 싶다면 이 부분도 편하게 말씀해주세요. $optionalContinuationQuestion",
                        ).filter(String::isNotBlank).joinToString(" ")
                    interpretation.isBlank() -> FREE_INPUT_GUIDE
                    else -> "$interpretation $FREE_INPUT_GUIDE"
                }
            } else {
                interpretation
            }
        val reflected =
            copy(
                action = ConversationTurnAction.REFLECT,
                acknowledgement = acknowledgement,
                interpretation = reflectInterpretation,
                question = null,
                questionTarget = null,
            )
        return if (reflected.content().isBlank()) {
            reflected.copy(acknowledgement = DEFAULT_REFLECT_ACKNOWLEDGEMENT)
        } else {
            reflected
        }
    }

    private fun String.hasContinuationGuide(): Boolean = CONTINUATION_GUIDE_PHRASES.any(::contains)

    private fun GeneratedConversationTurn.sanitizeUserFacingContent(userMessageContent: String?): GeneratedConversationTurn =
        copy(
            acknowledgement =
                acknowledgement.takeUnless { ConversationInternalMetadataPolicy.contains(it, userMessageContent) }.orEmpty(),
            interpretation =
                interpretation.takeUnless { ConversationInternalMetadataPolicy.contains(it, userMessageContent) }.orEmpty(),
            question = question?.takeUnless { ConversationInternalMetadataPolicy.contains(it, userMessageContent) },
        )

    private fun GeneratedConversationTurn.ensureVisibleContent(): GeneratedConversationTurn {
        if (content().isNotBlank()) return this
        return when (action) {
            ConversationTurnAction.CONFIRM_COMPLETION ->
                copy(
                    acknowledgement = "회고를 마칠까요?",
                    interpretation = "",
                    question = null,
                    questionTarget = null,
                )
            else -> asReflect()
        }
    }

    private fun fallbackQuestion(target: RetrospectiveItemType): String =
        when (target) {
            RetrospectiveItemType.FACT -> "오늘 진행한 일에서 더 남기고 싶은 내용은 무엇인가요?"
            RetrospectiveItemType.FEEL -> "그 일을 진행하면서 어떤 느낌이 들었나요?"
            RetrospectiveItemType.STRENGTH -> "이번 일에서 스스로 잘했다고 생각하는 점은 무엇인가요?"
            RetrospectiveItemType.BLOCK -> "진행하면서 어려웠거나 막혔던 점은 무엇이었나요?"
            RetrospectiveItemType.PROCESS -> "그 일을 어떤 방식으로 진행했나요?"
            RetrospectiveItemType.LEARN -> "이번 경험을 통해 새롭게 알게 된 점은 무엇인가요?"
            RetrospectiveItemType.ACTION -> "다음에는 무엇을 다르게 해보고 싶나요?"
        }

    fun applyAnalysisUpdates(
        items: List<ConversationV2AnalysisItemSnapshot>,
        updates: List<ConversationAnalysisUpdate>,
        messages: List<ConversationV2ConversationMessageSnapshot>,
    ): List<ConversationV2AnalysisUpdateDecision> {
        val currentItems = items.associateBy { it.itemType }.toMutableMap()
        return updates.map { update ->
            val evidence = evidenceDecision(messages, update.evidenceMessageIds)
            val before = currentItems[update.itemType]
            val after =
                if (before != null && evidence.acceptedMessageIds.isNotEmpty()) {
                    before
                        .copy(
                            status = maxOf(before.status, update.status),
                            summary = update.summary.trim().takeIf { it.isNotEmpty() },
                        ).also { currentItems[update.itemType] = it }
                } else {
                    before
                }
            ConversationV2AnalysisUpdateDecision(
                update = update,
                acceptedEvidenceMessageIds = evidence.acceptedMessageIds,
                rejectedEvidenceMessageIds = evidence.rejectedMessageIds,
                before = before,
                after = after,
                applied = before != null && evidence.acceptedMessageIds.isNotEmpty(),
            )
        }
    }
}

internal object ConversationInternalMetadataPolicy {
    private val internalEnumNames =
        (RetrospectiveItemType.entries.map { it.name } + RetrospectiveItemStatus.entries.map { it.name })
            .joinToString("|") { Regex.escape(it) }
    private val internalEnumPatterns =
        (RetrospectiveItemType.entries.map { it.name } + RetrospectiveItemStatus.entries.map { it.name })
            .associateWith { Regex("(?<![A-Za-z0-9_])${Regex.escape(it)}(?![A-Za-z0-9_])") }
    private val internalEnumToken = "(?<![A-Za-z0-9_])(?:$internalEnumNames)(?![A-Za-z0-9_])"
    private val internalMetaTerms =
        "(?:항목|비어|채워|수집\\s*상태|분석\\s*상태|회고\\s*상태|계획\\s*수준)"
    private val internalFieldNames =
        listOf(
            "analysisItems",
            "currentMessageId",
            "consecutiveIrrelevantCount",
            "eligibleQuestionTargets",
            "recentQuestionTargets",
            "completionRecommended",
            "completionPreviouslyOffered",
            "continuationRequested",
            "questionTarget",
            "declinedItemTypes",
            "analysisUpdates",
            "answerExamples",
            "evidenceMessageIds",
            "inputTokens",
            "outputTokens",
        ).joinToString("|") { Regex.escape(it) }
    private val patterns =
        listOf(
            Regex("$internalEnumToken(?=.{0,20}$internalMetaTerms)"),
            Regex("$internalMetaTerms.{0,20}$internalEnumToken"),
            Regex(
                "(?<![A-Za-z0-9_])(?:$internalFieldNames)(?![A-Za-z0-9_])",
                RegexOption.IGNORE_CASE,
            ),
            Regex("회고.{0,20}(?:비어|채워|수집|계획 수준)"),
            Regex("(?:회고|분석|수집)\\s*(?:항목|상태)"),
        )

    fun contains(
        content: String,
        userMessageContent: String? = null,
    ): Boolean =
        patterns.any { it.containsMatchIn(content) } ||
            internalEnumPatterns.values.any { pattern ->
                pattern.containsMatchIn(content) && userMessageContent?.let(pattern::containsMatchIn) != true
            }
}

data class ConversationV2MessageSnapshot(
    val content: String,
    val messageType: ConversationMessageType,
    val supportingContent: String? = null,
)

data class ConversationV2ConversationMessageSnapshot(
    val id: UUID,
    val sender: Sender,
    val relevance: MessageRelevance?,
)

data class ConversationV2AnalysisItemSnapshot(
    val itemType: RetrospectiveItemType,
    val status: RetrospectiveItemStatus,
    val summary: String?,
    val questionAllowed: Boolean = true,
)

data class ConversationV2EvidenceDecision(
    val acceptedMessageIds: List<UUID>,
    val rejectedMessageIds: List<UUID>,
)

data class ConversationV2AnalysisUpdateDecision(
    val update: ConversationAnalysisUpdate,
    val acceptedEvidenceMessageIds: List<UUID>,
    val rejectedEvidenceMessageIds: List<UUID>,
    val before: ConversationV2AnalysisItemSnapshot?,
    val after: ConversationV2AnalysisItemSnapshot?,
    val applied: Boolean,
)
