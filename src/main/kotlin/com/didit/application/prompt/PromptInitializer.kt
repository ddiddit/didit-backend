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
 * 앱 시작 시 prompts 테이블이 비어있으면 클래스패스 .txt 파일로 초기 데이터를 시딩한다.
 * 어드민에서 수정한 이후에는 DB 값이 우선 적용된다.
 */
@Component
class PromptInitializer(
    private val promptRepository: PromptRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(PromptInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        val existing = promptRepository.findAll()
        if (existing.isNotEmpty()) {
            logger.info("프롬프트 초기 데이터 이미 존재함 (${existing.size}개) — 시딩 생략")
            return
        }

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
