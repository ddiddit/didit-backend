package com.didit.domain.organization

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "projects")
@Entity
class Project(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    var userId: UUID,
    @Column(nullable = false, length = 15)
    var name: String,
    @Column
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    companion object {
        fun create(
            userId: UUID,
            name: String,
        ): Project {
            val normalizedName = name.trim()

            require(normalizedName.isNotBlank()) { "프로젝트 이름은 비어있을 수 없습니다." }
            require(normalizedName.length <= 15) { "프로젝트 이름은 15자를 초과할 수 없습니다." }

            return Project(
                userId = userId,
                name = normalizedName,
            )
        }
    }

    fun delete() {
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = deletedAt != null
}
