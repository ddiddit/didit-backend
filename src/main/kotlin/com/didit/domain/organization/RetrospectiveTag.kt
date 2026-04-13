package com.didit.domain.organization

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "retrospective_tags")
@Entity
class RetrospectiveTag(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val retrospectiveId: UUID,
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val tagId: UUID,
    @Column
    var isActive: Boolean? = true,
    @Column
    var deletedAt: LocalDateTime? = null,
) {
    fun isDeleted(): Boolean = this.deletedAt != null

    fun delete() {
        this.deletedAt = LocalDateTime.now()
        isActive = false
    }

    companion object {
        fun add(
            retrospectiveId: UUID,
            tagId: UUID,
        ): RetrospectiveTag =
            RetrospectiveTag(
                retrospectiveId = retrospectiveId,
                tagId = tagId,
                isActive = true,
            )
    }
}
