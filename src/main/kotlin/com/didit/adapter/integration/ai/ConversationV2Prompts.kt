package com.didit.adapter.integration.ai

import com.didit.application.prompt.required.PromptRepository
import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.shared.Job
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ConversationV2Prompts(
    private val promptRepository: PromptRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun build(request: ConversationTurnAIRequest): String {
        val jobType = request.job.toPromptJobType()
        val template =
            promptRepository.findByJobTypeAndPromptType(jobType, PromptType.CONVERSATION_V2)?.content
                ?: ClassPathResource("prompts/conversation-v2.txt").inputStream.bufferedReader().readText()
        return template.replace("{{context}}", objectMapper.writeValueAsString(request))
    }

    private fun Job?.toPromptJobType(): PromptJobType =
        when (this) {
            Job.PLANNER -> PromptJobType.PLANNER
            Job.DESIGNER -> PromptJobType.DESIGNER
            Job.DEVELOPER, null -> PromptJobType.DEVELOPER
        }
}
