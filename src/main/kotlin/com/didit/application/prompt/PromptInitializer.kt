package com.didit.application.prompt

import com.didit.application.prompt.required.PromptRepository
import com.didit.domain.prompt.Prompt
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 앱 시작 시 누락된 프롬프트 항목만 개별 시딩한다.
 * 이미 존재하는 항목은 건너뛰어 어드민 수정값을 보존한다.
 */
@Component
class PromptInitializer(
    private val promptRepository: PromptRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(PromptInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        val targets =
            listOf(
                Triple(PromptJobType.DEVELOPER, PromptType.DEEP_QUESTION, "prompts/deep-question-developer.txt"),
                Triple(PromptJobType.PLANNER, PromptType.DEEP_QUESTION, "prompts/deep-question-planner.txt"),
                Triple(PromptJobType.DESIGNER, PromptType.DEEP_QUESTION, "prompts/deep-question-designer.txt"),
                Triple(PromptJobType.DEVELOPER, PromptType.SUMMARY, "prompts/summary-developer.txt"),
                Triple(PromptJobType.PLANNER, PromptType.SUMMARY, "prompts/summary-planner.txt"),
                Triple(PromptJobType.DESIGNER, PromptType.SUMMARY, "prompts/summary-designer.txt"),
            )

        targets.forEach { (jobType, promptType, path) ->
            if (promptRepository.findByJobTypeAndPromptType(jobType, promptType) != null) {
                logger.debug("프롬프트 이미 존재함 — 시딩 생략: $jobType / $promptType")
                return@forEach
            }
            runCatching {
                val content = ClassPathResource(path).inputStream.bufferedReader().readText()
                promptRepository.save(Prompt(jobType = jobType, promptType = promptType, content = content))
                logger.info("프롬프트 시딩 완료 - $jobType / $promptType")
            }.onFailure {
                logger.error("프롬프트 시딩 실패 - path: $path", it)
            }
        }
    }
}
