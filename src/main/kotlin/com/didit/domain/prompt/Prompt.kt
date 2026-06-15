package com.didit.domain.prompt

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "prompts")
@Entity
class Prompt(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val jobType: PromptJobType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val promptType: PromptType,
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    var content: String,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column
    var updatedBy: String? = null,
) {
    fun update(
        content: String,
        updatedBy: String,
    ) {
        this.content = content
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }
}

enum class PromptJobType { DEVELOPER, PLANNER, DESIGNER }

enum class PromptType { DEEP_QUESTION, SUMMARY }
