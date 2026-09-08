package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.RetrospectiveMemoResult
import java.util.UUID

interface RetrospectiveMemoRegister {
    fun create(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
    ): RetrospectiveMemoResult
}

interface RetrospectiveMemoFinder {
    fun findAll(
        retrospectiveId: UUID,
        userId: UUID,
    ): List<RetrospectiveMemoResult>
}

interface RetrospectiveMemoModifier {
    fun update(
        retrospectiveId: UUID,
        memoId: UUID,
        userId: UUID,
        content: String,
    ): RetrospectiveMemoResult

    fun delete(
        retrospectiveId: UUID,
        memoId: UUID,
        userId: UUID,
    )
}
