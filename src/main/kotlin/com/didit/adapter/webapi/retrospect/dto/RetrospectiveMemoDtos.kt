package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.RetrospectiveMemoResult
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveMemoContentRequest(
    @field:NotBlank(message = "메모 내용은 비어 있을 수 없습니다.")
    val content: String,
)

data class RetrospectiveMemoResponse(
    val id: UUID,
    val content: String,
    val memoDate: LocalDate,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(result: RetrospectiveMemoResult): RetrospectiveMemoResponse =
            RetrospectiveMemoResponse(
                id = result.id,
                content = result.content,
                memoDate = result.memoDate,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
            )
    }
}
