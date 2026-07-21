package com.didit.adapter.integration.ai

import com.didit.application.prompt.required.PromptRepository
import com.didit.domain.prompt.Prompt
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.shared.Job
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

@SpringJUnitConfig(FeedbackPromptsTransactionTest.Config::class)
class FeedbackPromptsTransactionTest {
    @Autowired
    private lateinit var feedbackPrompts: FeedbackPrompts

    @Autowired
    private lateinit var promptRepository: PromptRepository

    @Test
    fun `buildSummaryPrompt - reads prompt in transaction and releases it before returning`() {
        whenever(promptRepository.findByJobTypeAndPromptType(PromptJobType.DEVELOPER, PromptType.SUMMARY)).thenAnswer {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue()
            Prompt(
                jobType = PromptJobType.DEVELOPER,
                promptType = PromptType.SUMMARY,
                content = "{{q1}} {{q2}} {{q3}} {{q4}} {{deepQuestion}}",
            )
        }

        val result = feedbackPrompts.buildSummaryPrompt(Job.DEVELOPER, listOf("1", "2", "3", "4"), "deep")

        assertThat(result).isEqualTo("1 2 3 4 deep")
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
    }

    @Test
    fun `buildDeepQuestionPrompt - reads prompt in transaction and releases it before returning`() {
        whenever(promptRepository.findByJobTypeAndPromptType(PromptJobType.DEVELOPER, PromptType.DEEP_QUESTION)).thenAnswer {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue()
            Prompt(
                jobType = PromptJobType.DEVELOPER,
                promptType = PromptType.DEEP_QUESTION,
                content = "{{q1}} {{q2}} {{q3}}",
            )
        }

        val result = feedbackPrompts.buildDeepQuestionPrompt(Job.DEVELOPER, listOf("1", "2", "3"))

        assertThat(result).isEqualTo("1 2 3")
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
    }

    @Configuration
    @EnableTransactionManagement
    class Config {
        @Bean
        fun promptRepository(): PromptRepository = mock()

        @Bean
        fun feedbackPrompts(promptRepository: PromptRepository) = FeedbackPrompts(promptRepository)

        @Bean
        fun dataSource(): DataSource =
            DriverManagerDataSource().apply {
                setDriverClassName("org.h2.Driver")
                url = "jdbc:h2:mem:feedback-prompts-transaction-test;DB_CLOSE_DELAY=-1"
                username = "sa"
                password = ""
            }

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)
    }
}
