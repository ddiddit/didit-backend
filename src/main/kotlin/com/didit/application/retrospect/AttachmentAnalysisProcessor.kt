package com.didit.application.retrospect

import com.didit.application.retrospect.required.AttachmentContentAnalyzer
import com.didit.application.retrospect.required.AttachmentStorage
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.domain.retrospect.AttachmentAnalysisStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Component
class AttachmentAnalysisProcessor(
    private val attachmentRepository: RetrospectiveAttachmentRepository,
    private val storage: AttachmentStorage,
    private val analyzer: AttachmentContentAnalyzer,
    private val sensitiveDataDetector: AttachmentSensitiveDataDetector,
    private val chatMessageRepository: ChatMessageRepository,
    private val conversationService: RetrospectiveConversationV2Service,
    private val transactionTemplate: TransactionTemplate,
) {
    fun process(event: AttachmentConversationRequestedEvent) {
        val plan = beginAnalysis(event.userMessageId, event.recovery)
        if (plan.hasActiveProcessing) return
        val work = plan.work
        if (work.isEmpty()) {
            conversationService.failAttachedTurn(event, ATTACHMENT_ANALYSIS_FAILED)
            return
        }

        var outcome =
            if (work.any { it.alreadyUnreadable }) {
                AnalysisBatchOutcome.UNREADABLE
            } else {
                AnalysisBatchOutcome.COMPLETED
            }
        var hasCompletedAttachment = work.any { it.alreadyCompleted }
        work.forEach { item ->
            if (item.alreadyCompleted || item.alreadyUnreadable) return@forEach
            try {
                val bytes = storage.read(item.storageKey)
                val extracted = analyzer.analyze(item.fileType, item.contentType, bytes)
                val containsSensitiveData = sensitiveDataDetector.containsSensitiveData(extracted)
                complete(
                    item.id,
                    sensitiveDataDetector.redact(extracted).takeIf { containsSensitiveData } ?: extracted,
                    containsSensitiveData,
                )
                hasCompletedAttachment = true
            } catch (exception: IllegalArgumentException) {
                outcome = AnalysisBatchOutcome.UNREADABLE
                markUnreadable(item.id)
                logger.info("읽을 수 없는 회고 첨부파일 - attachmentId: ${item.id}", exception)
            } catch (exception: Exception) {
                if (outcome != AnalysisBatchOutcome.UNREADABLE) outcome = AnalysisBatchOutcome.RETRYABLE_FAILURE
                fail(item.id)
                logger.error("회고 첨부파일 분석 실패 - attachmentId: ${item.id}", exception)
            }
        }

        when (outcome) {
            AnalysisBatchOutcome.UNREADABLE -> {
                val hasUserText = chatMessageRepository.findById(event.userMessageId)?.content?.isNotBlank() == true
                if (hasUserText || hasCompletedAttachment) {
                    conversationService.processAttachedTurn(event)
                } else {
                    conversationService.completeUnreadableAttachedTurn(event)
                }
            }
            AnalysisBatchOutcome.RETRYABLE_FAILURE -> conversationService.failAttachedTurn(event, ATTACHMENT_ANALYSIS_FAILED)
            AnalysisBatchOutcome.COMPLETED -> conversationService.processAttachedTurn(event)
        }
    }

    private fun beginAnalysis(
        messageId: UUID,
        recovery: Boolean,
    ): AnalysisBatchPlan =
        transactionTemplate.execute {
            val attachments = attachmentRepository.findAllByChatMessageIdForUpdate(messageId)
            val activeProcessing = attachments.any { it.analysisStatus == AttachmentAnalysisStatus.PROCESSING } && !recovery
            if (activeProcessing) return@execute AnalysisBatchPlan(emptyList(), hasActiveProcessing = true)
            AnalysisBatchPlan(
                attachments.map {
                    val completed = it.analysisStatus == AttachmentAnalysisStatus.COMPLETED
                    val unreadable = it.analysisStatus == AttachmentAnalysisStatus.UNREADABLE
                    when (it.analysisStatus) {
                        AttachmentAnalysisStatus.NOT_STARTED -> {
                            it.startAnalysis()
                            attachmentRepository.save(it)
                        }
                        AttachmentAnalysisStatus.FAILED_RETRYABLE -> {
                            it.retryAnalysis()
                            attachmentRepository.save(it)
                        }
                        AttachmentAnalysisStatus.PROCESSING -> {
                            if (recovery) {
                                it.restartStalledAnalysis()
                                attachmentRepository.save(it)
                            }
                        }
                        else -> Unit
                    }
                    AnalysisWork(it.id, it.storageKey, it.fileType, it.contentType, completed, unreadable)
                },
                activeProcessing,
            )
        }!!

    private fun complete(
        attachmentId: UUID,
        content: String,
        containsSensitiveData: Boolean,
    ) = transactionTemplate.executeWithoutResult {
        attachmentRepository.findById(attachmentId)?.also {
            it.completeAnalysis(content, containsSensitiveData)
            attachmentRepository.save(it)
        }
    }

    private fun fail(attachmentId: UUID) =
        transactionTemplate.executeWithoutResult {
            attachmentRepository.findById(attachmentId)?.also {
                it.failAnalysis(TEMPORARY_ANALYSIS_FAILURE)
                attachmentRepository.save(it)
            }
        }

    private fun markUnreadable(attachmentId: UUID) =
        transactionTemplate.executeWithoutResult {
            attachmentRepository.findById(attachmentId)?.also {
                it.markUnreadable(UNREADABLE_FILE)
                attachmentRepository.save(it)
            }
        }

    private data class AnalysisWork(
        val id: UUID,
        val storageKey: String,
        val fileType: com.didit.domain.retrospect.AttachmentFileType,
        val contentType: String,
        val alreadyCompleted: Boolean,
        val alreadyUnreadable: Boolean,
    )

    private data class AnalysisBatchPlan(
        val work: List<AnalysisWork>,
        val hasActiveProcessing: Boolean,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(AttachmentAnalysisProcessor::class.java)
        private const val ATTACHMENT_ANALYSIS_FAILED = "ATTACHMENT_ANALYSIS_FAILED"
        private const val TEMPORARY_ANALYSIS_FAILURE = "TEMPORARY_ANALYSIS_FAILURE"
        private const val UNREADABLE_FILE = "UNREADABLE_FILE"
    }

    private enum class AnalysisBatchOutcome {
        COMPLETED,
        RETRYABLE_FAILURE,
        UNREADABLE,
    }
}
