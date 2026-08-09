package com.didit.application.retrospect.required

import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.RetrospectiveAnalysisEvidence
import com.didit.domain.retrospect.RetrospectiveAnalysisItem
import com.didit.domain.retrospect.RetrospectiveConversationTurn
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ChatMessageRepository : Repository<ChatMessage, UUID> {
    fun save(message: ChatMessage): ChatMessage

    fun findById(id: UUID): ChatMessage?

    @Query("SELECT m FROM ChatMessage m WHERE m.retrospective.id = :retrospectiveId ORDER BY m.createdAt ASC")
    fun findAllByRetrospectiveIdOrderByCreatedAtAsc(
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
}
