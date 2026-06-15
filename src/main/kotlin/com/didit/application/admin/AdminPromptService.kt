package com.didit.application.admin

import com.didit.application.admin.provided.AdminPromptFinder
import com.didit.application.admin.provided.AdminPromptResult
import com.didit.application.prompt.required.PromptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminPromptService(
    private val promptRepository: PromptRepository,
) : AdminPromptFinder {
    @Transactional(readOnly = true)
    override fun findAll(): List<AdminPromptResult> =
        promptRepository.findAll().map { it.toResult() }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): AdminPromptResult {
        val prompt = promptRepository.findById(id) ?: throw NoSuchElementException("프롬프트를 찾을 수 없습니다: $id")
        return prompt.toResult()
    }

    @Transactional
    fun update(
        id: UUID,
        content: String,
        updatedBy: String,
    ): AdminPromptResult {
        val prompt = promptRepository.findById(id) ?: throw NoSuchElementException("프롬프트를 찾을 수 없습니다: $id")
        prompt.update(content = content, updatedBy = updatedBy)
        return promptRepository.save(prompt).toResult()
    }

    private fun com.didit.domain.prompt.Prompt.toResult() =
        AdminPromptResult(
            id = id,
            jobType = jobType.name,
            promptType = promptType.name,
            content = content,
            updatedAt = updatedAt,
            updatedBy = updatedBy,
        )
}
