package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "retrospective_summaries")
@Entity
class RetrospectiveSummary(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrospective_id", nullable = false)
    val retrospective: Retrospective,
    @Lob
    @Column(name = "summary_content", nullable = false, columnDefinition = "TEXT")
    val summaryContent: String,
    @Column(name = "generated_at", nullable = false)
    val generatedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity() {
    companion object {
        fun create(
            retrospective: Retrospective,
            summaryContent: String,
        ): RetrospectiveSummary =
            RetrospectiveSummary(
                retrospective = retrospective,
                summaryContent = summaryContent,
                generatedAt = LocalDateTime.now(),
            )
    }
}
