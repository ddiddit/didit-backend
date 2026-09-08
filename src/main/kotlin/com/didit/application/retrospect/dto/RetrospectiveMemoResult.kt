package com.didit.application.retrospect.dto

import com.didit.domain.retrospect.RetrospectiveMemo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveMemoResult(
    val id: UUID,
    val content: String,
    val memoDate: LocalDate,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(memo: RetrospectiveMemo): RetrospectiveMemoResult =
            RetrospectiveMemoResult(
                id = memo.id,
                content = memo.content,
                memoDate = memo.memoDate,
                createdAt = memo.createdAt,
                updatedAt = memo.updatedAt,
            )
    }
}
