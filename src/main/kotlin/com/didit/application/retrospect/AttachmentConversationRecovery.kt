package com.didit.application.retrospect

import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.application.retrospect.required.RetrospectiveConversationTurnRepository
import com.didit.domain.retrospect.ConversationTurnStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AttachmentConversationRecovery(
    private val turnRepository: RetrospectiveConversationTurnRepository,
    private val attachmentRepository: RetrospectiveAttachmentRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun recoverStale() {
        turnRepository
            .findAllByStatusAndUpdatedAtBefore(ConversationTurnStatus.PROCESSING, LocalDateTime.now().minusMinutes(5))
            .forEach { turn ->
                val attachments = attachmentRepository.findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(turn.userMessageId)
                val owner = attachments.firstOrNull() ?: return@forEach
                eventPublisher.publishEvent(
                    AttachmentConversationRequestedEvent(
                        retrospectiveId = turn.retrospectiveId,
                        userId = owner.userId,
                        turnId = turn.id,
                        userMessageId = turn.userMessageId,
                        recovery = true,
                    ),
                )
            }
    }
}
