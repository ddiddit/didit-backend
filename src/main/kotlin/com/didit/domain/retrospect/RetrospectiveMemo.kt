package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.util.UUID

@Table(
    name = "retrospective_memos",
    uniqueConstraints = [UniqueConstraint(columnNames = ["retrospective_id", "memo_date"])],
)
@Entity
class RetrospectiveMemo(
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val retrospectiveId: UUID,
    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,
    @Column(name = "memo_date", nullable = false)
    val memoDate: LocalDate,
    id: UUID = UUID.randomUUID(),
) : BaseEntity() {
    @jakarta.persistence.Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = id

    fun update(content: String) {
        this.content = content
    }

    companion object {
        fun create(
            retrospectiveId: UUID,
            content: String,
            memoDate: LocalDate,
        ): RetrospectiveMemo =
            RetrospectiveMemo(
                retrospectiveId = retrospectiveId,
                content = content,
                memoDate = memoDate,
            )
    }
}
