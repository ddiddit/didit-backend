package com.didit.application.retrospect

import com.didit.application.retrospect.dto.GetRetrospectiveResponse
import com.didit.application.retrospect.dto.CompleteRetrospectiveResponse
import com.didit.application.retrospect.dto.ChatMessageDto
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.RetrospectiveSummaryRepository
import com.didit.application.retrospect.required.UserFinder
import com.didit.application.retrospect.required.AIClient
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectQueryService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val retrospectiveSummaryRepository: RetrospectiveSummaryRepository,
    private val userFinder: UserFinder,
) {
    
    fun getRetrospective(userId: UUID): GetRetrospectiveResponse {
        val retrospective = retrospectiveRepository.findByUserIdWithChatMessages(userId)
            ?: throw IllegalArgumentException("진행 중인 회고가 없습니다.")
        
        val summary = retrospectiveSummaryRepository.findByRetrospectiveId(retrospective.id)
        
        return GetRetrospectiveResponse(
            retrospectiveId = retrospective.id.toString(),
            currentQuestionNumber = retrospective.currentQuestionNumber,
            isCompleted = retrospective.isCompleted,
            chatHistory = retrospective.chatMessages.map { message ->
                ChatMessageDto(
                    questionNumber = message.questionNumber,
                    content = message.content,
                    isAnswer = message.isAnswer,
                    isDeepQuestion = message.isDeepQuestion,
                    createdAt = message.messageCreatedAt.toString()
                )
            },
            summary = summary?.summaryContent
        )
    }
    
    fun completeRetrospective(userId: UUID): CompleteRetrospectiveResponse {
        val retrospective = retrospectiveRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("회고를 찾을 수 없습니다.")
        
        if (!retrospective.isCompleted) {
            throw IllegalStateException("아직 완료되지 않은 회고입니다.")
        }
        
        val summary = retrospectiveSummaryRepository.findByRetrospectiveId(retrospective.id)
            ?: throw IllegalStateException("회고 요약을 찾을 수 없습니다.")
        
        return CompleteRetrospectiveResponse(
            retrospectiveId = retrospective.id.toString(),
            summary = summary.summaryContent
        )
    }
}
