package com.didit.application.retrospect

import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.retrospect.ConversationAnswerGuideMode
import com.didit.domain.retrospect.ConversationTurnAction
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import org.springframework.stereotype.Component

@Component
class ConversationAnswerGuidePolicy(
    private val sensitiveDataDetector: AttachmentSensitiveDataDetector,
) {
    companion object {
        private const val MIN_EXAMPLE_COUNT = 2
        private const val MAX_EXAMPLE_COUNT = 3
        private const val MAX_EXAMPLE_LENGTH = 60
        private val SUPPORTED_TARGETS =
            setOf(
                RetrospectiveItemType.BLOCK,
                RetrospectiveItemType.STRENGTH,
                RetrospectiveItemType.LEARN,
                RetrospectiveItemType.ACTION,
            )
        private val ACTION_MAINTENANCE_TERMS = listOf("유지", "그대로", "계속", "바꾸지 않")
    }

    fun decide(
        generated: GeneratedConversationTurn,
        decided: GeneratedConversationTurn,
        items: List<ConversationV2AnalysisItemSnapshot>,
        sensitiveDataDetected: Boolean,
        previouslyAskedTargets: Set<RetrospectiveItemType> = emptySet(),
        userMessageContent: String? = null,
    ): ConversationAnswerGuide? {
        val target = decided.questionTarget ?: return null
        if (decided.action != ConversationTurnAction.ASK || target !in SUPPORTED_TARGETS) return null
        if (generated.action != ConversationTurnAction.ASK || generated.questionTarget != target) return null
        if (target in previouslyAskedTargets) return null

        val statuses = items.associate { it.itemType to it.status }
        if (statuses[target] != RetrospectiveItemStatus.EMPTY || !hasRequiredContext(target, statuses)) return null

        val examples =
            generated.answerExamples.map { example ->
                example.trim().let { if (sensitiveDataDetected) sensitiveDataDetector.redact(it) else it }
            }
        if (examples.size !in MIN_EXAMPLE_COUNT..MAX_EXAMPLE_COUNT) return null
        if (examples.any { it.isBlank() || it.length > MAX_EXAMPLE_LENGTH || '\n' in it || '\r' in it }) return null
        if (examples.any { ConversationInternalMetadataPolicy.contains(it, userMessageContent) }) return null
        if (examples.distinct().size != examples.size) return null
        if (target == RetrospectiveItemType.ACTION && examples.none(::isMaintenanceChoice)) return null

        return ConversationAnswerGuide(
            mode =
                if (target == RetrospectiveItemType.ACTION) {
                    ConversationAnswerGuideMode.INLINE
                } else {
                    ConversationAnswerGuideMode.ON_DEMAND
                },
            examples = examples,
        )
    }

    private fun hasRequiredContext(
        target: RetrospectiveItemType,
        statuses: Map<RetrospectiveItemType, RetrospectiveItemStatus>,
    ): Boolean {
        fun collected(type: RetrospectiveItemType) = statuses[type]?.let { it != RetrospectiveItemStatus.EMPTY } == true
        if (!collected(RetrospectiveItemType.FACT)) return false
        if (target != RetrospectiveItemType.ACTION) return true
        return listOf(
            RetrospectiveItemType.PROCESS,
            RetrospectiveItemType.BLOCK,
            RetrospectiveItemType.STRENGTH,
            RetrospectiveItemType.LEARN,
            RetrospectiveItemType.FEEL,
        ).any(::collected)
    }

    private fun isMaintenanceChoice(example: String): Boolean = ACTION_MAINTENANCE_TERMS.any(example::contains)
}

data class ConversationAnswerGuide(
    val mode: ConversationAnswerGuideMode,
    val examples: List<String>,
)
