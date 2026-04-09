package com.didit.domain.organization

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "tags")
@Entity
class Tag(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    var name: String,
    @Column
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    companion object {
        fun create(
            userId: UUID,
            name: String,
        ): Tag {
            val normalized = name.trim()

            require(normalized.isNotBlank()) { "태그명은 비어있을 수 없습니다." }

            return Tag(
                userId = userId,
                name = normalized,
            )
        }
    }

    fun delete() {
        this.deletedAt = LocalDateTime.now()
    }
}
