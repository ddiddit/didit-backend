package com.didit.adapter.integration.ai

import com.didit.application.prompt.required.PromptRepository
import com.didit.application.retrospect.required.RetrospectiveResultV2AIRequest
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.shared.Job
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RetrospectiveResultV2Prompts(
    private val promptRepository: PromptRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun build(request: RetrospectiveResultV2AIRequest): String {
        val template =
            promptRepository.findByJobTypeAndPromptType(request.job.toPromptJobType(), PromptType.RESULT_V2)?.content
                ?: ClassPathResource("prompts/result-v2.txt").inputStream.bufferedReader().readText()
        return template.replace("{{context}}", objectMapper.writeValueAsString(request))
    }

    private fun Job?.toPromptJobType(): PromptJobType =
        when (this) {
            Job.PLANNER -> PromptJobType.PLANNER
            Job.DESIGNER -> PromptJobType.DESIGNER
            Job.DEVELOPER, null -> PromptJobType.DEVELOPER
        }
}
