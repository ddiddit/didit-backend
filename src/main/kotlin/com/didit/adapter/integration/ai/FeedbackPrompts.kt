package com.didit.adapter.integration.ai

import com.didit.domain.shared.Job
import org.springframework.core.io.ClassPathResource

object FeedbackPrompts {
    fun buildDeepQuestionPrompt(
        job: Job?,
        answers: List<String>,
    ): String {
        val template = loadPrompt("prompts/deep-question-${job?.name?.lowercase()}.txt")
        return template
            .replace("{{q1}}", answers.getOrElse(0) { "" })
            .replace("{{q2}}", answers.getOrElse(1) { "" })
            .replace("{{q3}}", answers.getOrElse(2) { "" })
    }

    fun buildSummaryPrompt(
        job: Job?,
        allAnswers: List<String>,
    ): String {
        val template = loadPrompt("prompts/summary-${job?.name?.lowercase()}.txt")
        return template
            .replace("{{q1}}", allAnswers.getOrElse(0) { "" })
            .replace("{{q2}}", allAnswers.getOrElse(1) { "" })
            .replace("{{q3}}", allAnswers.getOrElse(2) { "" })
            .replace("{{q4}}", allAnswers.getOrElse(3) { "" })
    }

    private fun loadPrompt(path: String): String = ClassPathResource(path).inputStream.bufferedReader().readText()
}
