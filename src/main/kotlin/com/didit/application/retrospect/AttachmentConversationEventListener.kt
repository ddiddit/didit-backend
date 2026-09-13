package com.didit.application.retrospect

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.event.EventListener
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component

@Component
class AttachmentConversationEventListener(
    private val processor: AttachmentAnalysisProcessor,
    @Qualifier("aiTaskExecutor") private val taskExecutor: TaskExecutor,
) {
    @EventListener
    fun on(event: AttachmentConversationRequestedEvent) {
        try {
            taskExecutor.execute { processor.process(event) }
        } catch (exception: TaskRejectedException) {
            logger.error("첨부파일 분석 작업 거절 - turnId: ${event.turnId}", exception)
            processor.process(event)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AttachmentConversationEventListener::class.java)
    }
}
