package com.didit.adapter.retrospect.out.persistence.mapper

import com.didit.adapter.retrospect.out.persistence.entity.ChatMessageJpaEntity
import com.didit.adapter.retrospect.out.persistence.entity.RetrospectiveJpaEntity
import com.didit.domain.retrospect.entity.ChatMessage
import com.didit.domain.retrospect.entity.Retrospective
import com.didit.domain.retrospect.model.RetrospectiveSummary
import org.springframework.stereotype.Component

@Component
class RetrospectivePersistenceMapper {

    fun toJpaEntity(domain: Retrospective): RetrospectiveJpaEntity {
        val retrospectiveJpaEntity = RetrospectiveJpaEntity(
            id = domain.id,
            userId = domain.userId,
            projectId = domain.projectId,
            title = domain.title,
            status = domain.status,
            inputTokens = domain.inputTokens,
            outputTokens = domain.outputTokens,
            doneWork = domain.summary?.doneWork,
            blockedPoint = domain.summary?.blockedPoint,
            solutionProcess = domain.summary?.solutionProcess,
            lessonLearned = domain.summary?.lessonLearned,
            insight = domain.summary?.insight,
            improvementDirection = domain.summary?.improvementDirection
        )

        retrospectiveJpaEntity.tagIds = domain.tagIds.toMutableList()

        val chatMessageEntities = domain.chatMessages.map { message ->
            ChatMessageJpaEntity(
                id = message.id,
                sender = message.sender,
                content = message.content,
                questionType = message.questionType,
                isSkipped = message.isSkipped,
                createdAt = message.createdAt,
                retrospective = retrospectiveJpaEntity
            )
        }

        retrospectiveJpaEntity.replaceChatMessages(chatMessageEntities)
        return retrospectiveJpaEntity
    }

    fun toDomain(entity: RetrospectiveJpaEntity): Retrospective {
        val summary = if (
            entity.doneWork != null ||
            entity.blockedPoint != null ||
            entity.solutionProcess != null ||
            entity.lessonLearned != null ||
            entity.insight != null ||
            entity.improvementDirection != null
        ) {
            RetrospectiveSummary(
                doneWork = entity.doneWork ?: "",
                blockedPoint = entity.blockedPoint ?: "",
                solutionProcess = entity.solutionProcess ?: "",
                lessonLearned = entity.lessonLearned ?: "",
                insight = entity.insight ?: "",
                improvementDirection = entity.improvementDirection ?: ""
            )
        } else {
            null
        }

        val chatMessages = entity.chatMessages.map { message ->
            ChatMessage(
                id = message.id,
                sender = message.sender,
                content = message.content,
                questionType = message.questionType,
                isSkipped = message.isSkipped,
                createdAt = message.createdAt
            )
        }.toMutableList()

        return Retrospective(
            id = entity.id,
            userId = entity.userId,
            projectId = entity.projectId,
            tagIds = entity.tagIds.toMutableList(),
            title = entity.title,
            status = entity.status,
            inputTokens = entity.inputTokens,
            outputTokens = entity.outputTokens,
            chatMessages = chatMessages,
            summary = summary
        )
    }
}