package com.didit.application.retrospect

import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.retrospect.dto.DeepQuestionResponse
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.provided.SearchHistoryRegister
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.Sender
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectQueryService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val searchHistoryRegister: SearchHistoryRegister,
    private val projectRepository: ProjectRepository,
) : RetrospectiveFinder {
    override fun findById(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective =
        retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)
            ?: throw RetrospectiveNotFoundException(retrospectiveId)

    override fun findAllByUserId(userId: UUID): List<Retrospective> = retrospectiveRepository.findAllCompletedByUserId(userId)

    override fun findRecentByUserId(
        userId: UUID,
        limit: Int,
    ): List<Retrospective> =
        retrospectiveRepository.findRecentCompletedByUserId(
            userId = userId,
            pageable = PageRequest.of(0, limit),
        )

    override fun countByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): Int =
        retrospectiveRepository.countByUserIdAndStatusNotAndCreatedAtBetween(
            userId = userId,
            status = RetroStatus.PENDING,
            from = date.atStartOfDay(),
            to = date.atTime(23, 59, 59),
        )

    override fun findByUserIdAndYearMonth(
        userId: UUID,
        year: Int,
        month: Int,
    ): List<Retrospective> {
        val from = LocalDate.of(year, month, 1).atStartOfDay()
        val to = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay()
        return retrospectiveRepository.findCompletedByUserIdAndPeriod(userId, from, to)
    }

    override fun findByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): List<Retrospective> =
        retrospectiveRepository.findCompletedByUserIdAndPeriod(
            userId = userId,
            from = date.atStartOfDay(),
            to = date.atTime(23, 59, 59),
        )

    override fun findByUserIdAndCurrentWeek(userId: UUID): List<Retrospective> {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return retrospectiveRepository.findCompletedByUserIdAndPeriod(
            userId = userId,
            from = weekStart.atStartOfDay(),
            to = weekEnd.atTime(23, 59, 59),
        )
    }

    override fun findDeepQuestion(
        retrospectiveId: UUID,
        userId: UUID,
    ): DeepQuestionResponse {
        val retrospective = findById(retrospectiveId, userId)
        val deepQuestion =
            retrospective.chatMessages
                .find { it.questionType == QuestionType.Q4_DEEP && it.sender == Sender.AI }

        return DeepQuestionResponse(
            isReady = deepQuestion != null,
            content = deepQuestion?.content,
        )
    }

    override fun searchByTitle(
        userId: UUID,
        keyword: String,
    ): List<Retrospective> {
        searchHistoryRegister.register(userId, keyword)
        return retrospectiveRepository.searchByUserIdAndTitle(userId, keyword)
    }

    override fun findByProject(
        userId: UUID,
        projectId: UUID,
    ): List<Retrospective> {
        projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)
            ?: throw ProjectNotFoundException(projectId)

        return retrospectiveRepository.findAllByUserIdAndProjectId(userId, projectId)
    }
}
