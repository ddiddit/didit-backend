package com.didit.application.retrospect

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DeepQuestionEventListener(
    private val processor: DeepQuestionProcessor,
    private val metrics: RetrospectiveAiMetrics,
    @Qualifier("aiTaskExecutor") private val aiTaskExecutor: TaskExecutor,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DeepQuestionEventListener::class.java)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: DeepQuestionGenerationEvent) {
        try {
            aiTaskExecutor.execute { processor.process(event) }
        } catch (exception: TaskRejectedException) {
            metrics.incrementRejected()
            logger.warn("AI executor 포화로 기본 심화 질문을 저장합니다. retrospectiveId: ${event.retrospectiveId}", exception)
            processor.saveFallback(event)
        }
    }
}
