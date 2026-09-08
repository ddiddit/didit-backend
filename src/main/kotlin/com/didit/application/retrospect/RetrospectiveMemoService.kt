package com.didit.application.retrospect

import com.didit.application.retrospect.dto.RetrospectiveMemoResult
import com.didit.application.retrospect.exception.DuplicateRetrospectiveMemoException
import com.didit.application.retrospect.exception.InvalidRetrospectiveMemoContentException
import com.didit.application.retrospect.exception.RetrospectiveMemoNotFoundException
import com.didit.application.retrospect.exception.SummaryNotGeneratedException
import com.didit.application.retrospect.provided.RetrospectiveMemoFinder
import com.didit.application.retrospect.provided.RetrospectiveMemoModifier
import com.didit.application.retrospect.provided.RetrospectiveMemoRegister
import com.didit.application.retrospect.required.RetrospectiveMemoRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveMemo
import com.didit.domain.retrospect.SummaryGenerationStatus
import com.didit.domain.shared.ServiceTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectiveMemoService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val retrospectiveMemoRepository: RetrospectiveMemoRepository,
) : RetrospectiveMemoRegister,
    RetrospectiveMemoFinder,
    RetrospectiveMemoModifier {
    @Transactional
    override fun create(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
    ): RetrospectiveMemoResult {
        findCompletedRetrospective(retrospectiveId, userId, forUpdate = true)
        val memoDate = ServiceTime.today()
        if (retrospectiveMemoRepository.existsByRetrospectiveIdAndMemoDate(retrospectiveId, memoDate)) {
            throw DuplicateRetrospectiveMemoException(retrospectiveId, memoDate)
        }

        return RetrospectiveMemoResult.from(
            retrospectiveMemoRepository.save(
                RetrospectiveMemo.create(
                    retrospectiveId = retrospectiveId,
                    content = normalizeContent(content),
                    memoDate = memoDate,
                ),
            ),
        )
    }

    override fun findAll(
        retrospectiveId: UUID,
        userId: UUID,
    ): List<RetrospectiveMemoResult> {
        findCompletedRetrospective(retrospectiveId, userId)
        return retrospectiveMemoRepository.findAllByRetrospectiveIdOrderByMemoDateDesc(retrospectiveId).map(RetrospectiveMemoResult::from)
    }

    @Transactional
    override fun update(
        retrospectiveId: UUID,
        memoId: UUID,
        userId: UUID,
        content: String,
    ): RetrospectiveMemoResult {
        findCompletedRetrospective(retrospectiveId, userId)
        val memo = findMemo(memoId, retrospectiveId)
        memo.update(normalizeContent(content))
        return RetrospectiveMemoResult.from(memo)
    }

    @Transactional
    override fun delete(
        retrospectiveId: UUID,
        memoId: UUID,
        userId: UUID,
    ) {
        findCompletedRetrospective(retrospectiveId, userId)
        retrospectiveMemoRepository.delete(findMemo(memoId, retrospectiveId))
    }

    private fun findCompletedRetrospective(
        retrospectiveId: UUID,
        userId: UUID,
        forUpdate: Boolean = false,
    ): Retrospective {
        val retrospective =
            if (forUpdate) {
                retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)
            } else {
                retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)
            }
                ?: throw com.didit.application.retrospect.exception
                    .RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isCompleted() || retrospective.summaryGenerationStatus != SummaryGenerationStatus.GENERATED) {
            throw SummaryNotGeneratedException(retrospectiveId)
        }
        return retrospective
    }

    private fun findMemo(
        memoId: UUID,
        retrospectiveId: UUID,
    ): RetrospectiveMemo =
        retrospectiveMemoRepository.findByIdAndRetrospectiveId(memoId, retrospectiveId)
            ?: throw RetrospectiveMemoNotFoundException(memoId)

    private fun normalizeContent(content: String): String {
        val normalized = content.trim()
        if (normalized.isBlank()) throw InvalidRetrospectiveMemoContentException()
        return normalized
    }
}
