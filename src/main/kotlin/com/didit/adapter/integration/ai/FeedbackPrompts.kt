package com.didit.adapter.integration.ai

import com.didit.application.prompt.required.PromptRepository
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.shared.Job
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class FeedbackPrompts(
    private val promptRepository: PromptRepository,
) {
    private val logger = LoggerFactory.getLogger(FeedbackPrompts::class.java)

    fun buildDeepQuestionPrompt(
        job: Job?,
        answers: List<String>,
    ): String {
        val template = loadTemplate(job, PromptType.DEEP_QUESTION)
        return template
            .replace("{{q1}}", answers.getOrElse(0) { "" })
            .replace("{{q2}}", answers.getOrElse(1) { "" })
            .replace("{{q3}}", answers.getOrElse(2) { "" })
    }

    fun buildSummaryPrompt(
        job: Job?,
        allAnswers: List<String>,
        deepQuestion: String? = null,
    ): String {
        val template = loadTemplate(job, PromptType.SUMMARY)
        return template
            .replace("{{q1}}", allAnswers.getOrElse(0) { "" })
            .replace("{{q2}}", allAnswers.getOrElse(1) { "" })
            .replace("{{q3}}", allAnswers.getOrElse(2) { "" })
            .replace("{{q4}}", allAnswers.getOrElse(3) { "" })
            .replace("{{deepQuestion}}", deepQuestion ?: "심화 질문")
    }

    private fun loadTemplate(
        job: Job?,
        promptType: PromptType,
    ): String {
        val jobType = job?.toPromptJobType() ?: PromptJobType.DEVELOPER

        val dbPrompt = promptRepository.findByJobTypeAndPromptType(jobType, promptType)
        if (dbPrompt != null) {
            logger.debug("DB 프롬프트 로드 - jobType: $jobType, promptType: $promptType")
            return dbPrompt.content
        }

        val path = "prompts/${promptType.toFileName()}-${jobType.name.lowercase()}.txt"
        return runCatching {
            ClassPathResource(path).inputStream.bufferedReader().readText()
        }.getOrElse {
            logger.error("프롬프트 파일 로드 실패 - path: $path", it)
            throw it
        }
    }

    private fun PromptType.toFileName() =
        when (this) {
            PromptType.DEEP_QUESTION -> "deep-question"
            PromptType.SUMMARY -> "summary"
        }

    private fun Job.toPromptJobType() =
        when (this) {
            Job.DEVELOPER -> PromptJobType.DEVELOPER
            Job.PLANNER -> PromptJobType.PLANNER
            Job.DESIGNER -> PromptJobType.DESIGNER
        }
}
