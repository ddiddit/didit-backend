package com.didit.application.retrospect.required

import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.RetrospectiveAnalysisEvidence
import com.didit.domain.retrospect.RetrospectiveAnalysisItem
import com.didit.domain.retrospect.RetrospectiveAttachment
import com.didit.domain.retrospect.RetrospectiveConversationTurn
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface ChatMessageRepository : Repository<ChatMessage, UUID> {
    fun save(message: ChatMessage): ChatMessage

    fun findById(id: UUID): ChatMessage?

    @Query("SELECT m FROM ChatMessage m WHERE m.retrospective.id = :retrospectiveId ORDER BY m.createdAt ASC")
    fun findAllByRetrospectiveIdOrderByCreatedAtAsc(
        @Param("retrospectiveId") retrospectiveId: UUID,
    ): List<ChatMessage>

    @Query(
        """
        SELECT m FROM ChatMessage m
        WHERE m.retrospective.id = :retrospectiveId
        AND m.sender = com.didit.domain.retrospect.Sender.USER
        AND m.includedInResult = true
        ORDER BY m.createdAt ASC
        """,
    )
    fun findAllResultEvidenceByRetrospectiveId(
        @Param("retrospectiveId") retrospectiveId: UUID,
    ): List<ChatMessage>

    fun findAllByIdIn(ids: Collection<UUID>): List<ChatMessage>
}

interface RetrospectiveAnalysisItemRepository : Repository<RetrospectiveAnalysisItem, UUID> {
    fun save(item: RetrospectiveAnalysisItem): RetrospectiveAnalysisItem

    fun saveAll(items: Iterable<RetrospectiveAnalysisItem>): List<RetrospectiveAnalysisItem>

    fun findAllByRetrospectiveIdOrderByItemTypeAsc(retrospectiveId: UUID): List<RetrospectiveAnalysisItem>
}

interface RetrospectiveAnalysisEvidenceRepository : Repository<RetrospectiveAnalysisEvidence, UUID> {
    fun saveAll(evidences: Iterable<RetrospectiveAnalysisEvidence>): List<RetrospectiveAnalysisEvidence>

    fun existsByAnalysisItemIdAndMessageId(
        analysisItemId: UUID,
        messageId: UUID,
    ): Boolean
}

interface RetrospectiveConversationTurnRepository : Repository<RetrospectiveConversationTurn, UUID> {
    fun save(turn: RetrospectiveConversationTurn): RetrospectiveConversationTurn

    fun findByRetrospectiveIdAndClientMessageId(
        retrospectiveId: UUID,
        clientMessageId: UUID,
    ): RetrospectiveConversationTurn?

    fun findFirstByRetrospectiveIdAndStatus(
        retrospectiveId: UUID,
        status: ConversationTurnStatus,
    ): RetrospectiveConversationTurn?

    fun findAllByRetrospectiveIdOrderByTurnNumberAsc(retrospectiveId: UUID): List<RetrospectiveConversationTurn>

    fun countByRetrospectiveId(retrospectiveId: UUID): Int

    fun findByUserMessageId(userMessageId: UUID): RetrospectiveConversationTurn?

    fun findAllByStatusAndUpdatedAtBefore(
        status: ConversationTurnStatus,
        updatedAt: LocalDateTime,
    ): List<RetrospectiveConversationTurn>
}

interface RetrospectiveAttachmentRepository : Repository<RetrospectiveAttachment, UUID> {
    fun save(attachment: RetrospectiveAttachment): RetrospectiveAttachment

    fun delete(attachment: RetrospectiveAttachment)

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): RetrospectiveAttachment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM RetrospectiveAttachment a WHERE a.id = :id AND a.userId = :userId")
    fun findByIdAndUserIdForUpdate(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
    ): RetrospectiveAttachment?

    fun findById(id: UUID): RetrospectiveAttachment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllByIdInAndUserIdAndRetrospectiveId(
        ids: Collection<UUID>,
        userId: UUID,
        retrospectiveId: UUID,
    ): List<RetrospectiveAttachment>

    fun findAllByChatMessageIdAndDeletedAtIsNullOrderByCreatedAtAsc(chatMessageId: UUID): List<RetrospectiveAttachment>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT a FROM RetrospectiveAttachment a
        WHERE a.chatMessageId = :chatMessageId AND a.deletedAt IS NULL
        ORDER BY a.createdAt ASC
        """,
    )
    fun findAllByChatMessageIdForUpdate(
        @Param("chatMessageId") chatMessageId: UUID,
    ): List<RetrospectiveAttachment>

    fun findAllByUploadStatus(uploadStatus: com.didit.domain.retrospect.AttachmentUploadStatus): List<RetrospectiveAttachment>

    fun findAllByChatMessageIdIsNullAndExpiresAtBeforeAndDeletedAtIsNull(expiresAt: LocalDateTime): List<RetrospectiveAttachment>

    @Query(
        """
        SELECT a FROM RetrospectiveAttachment a, Retrospective r
        WHERE a.retrospectiveId = r.id
        AND a.deletedAt IS NULL
        AND r.deletedAt IS NOT NULL
        """,
    )
    fun findAllForDeletedRetrospectives(): List<RetrospectiveAttachment>
}
