package com.didit.application.prompt.required

import com.didit.domain.prompt.Prompt
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import org.springframework.data.repository.Repository
import java.util.UUID

interface PromptRepository : Repository<Prompt, UUID> {
    fun findAll(): List<Prompt>

    fun findByJobTypeAndPromptType(
        jobType: PromptJobType,
        promptType: PromptType,
    ): Prompt?

    fun findById(id: UUID): Prompt?

    fun save(prompt: Prompt): Prompt
}
