package com.didit.adapter.integration.ai

import com.didit.application.retrospect.required.AIClient
import com.didit.domain.shared.Job
import org.springframework.stereotype.Component

@Component
class ClovaClient : AIClient {
    override fun generateDeepQuestion(
        job: Job?,
        answers: List<String>,
    ): String {
        TODO("구현 예정")
    }

    override fun generateSummary(
        job: Job?,
        allAnswers: List<String>,
    ): String {
        TODO("구현 예정")
    }
}
