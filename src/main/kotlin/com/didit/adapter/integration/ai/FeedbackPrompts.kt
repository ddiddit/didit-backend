package com.didit.adapter.integration.ai

import com.didit.domain.shared.Job
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

object FeedbackPrompts {
    private val logger = LoggerFactory.getLogger(FeedbackPrompts::class.java)

    fun buildDeepQuestionPrompt(
        job: Job?,
        answers: List<String>,
    ): String {
        val path = "prompts/deep-question-${job?.name?.lowercase()}.txt"
        val template = loadPrompt(path)
        val prompt =
            template
                .replace("{{q1}}", answers.getOrElse(0) { "" })
                .replace("{{q2}}", answers.getOrElse(1) { "" })
                .replace("{{q3}}", answers.getOrElse(2) { "" })

        logger.debug("심화 질문 프롬프트 로드 - path: $path")
        logger.debug("심화 질문 프롬프트 내용:\n$prompt")

        return prompt
    }

    fun buildSummaryPrompt(
        job: Job?,
        allAnswers: List<String>,
    ): String {
        val path = "prompts/summary-${job?.name?.lowercase()}.txt"
        val template = loadPrompt(path)
        val prompt =
            template
                .replace("{{q1}}", allAnswers.getOrElse(0) { "" })
                .replace("{{q2}}", allAnswers.getOrElse(1) { "" })
                .replace("{{q3}}", allAnswers.getOrElse(2) { "" })
                .replace("{{q4}}", allAnswers.getOrElse(3) { "" })

        logger.debug("요약 프롬프트 로드 - path: $path")
        logger.debug("요약 프롬프트 내용:\n$prompt")

        return prompt
    }

    private fun loadPrompt(path: String): String =
        runCatching {
            ClassPathResource(path).inputStream.bufferedReader().readText()
        }.getOrElse {
            logger.error("프롬프트 파일 로드 실패 - path: $path", it)
            throw it
        }
}
