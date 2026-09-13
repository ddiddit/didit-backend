package com.didit.application.retrospect

import com.didit.application.retrospect.required.AttachmentStorage
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.domain.retrospect.AttachmentUploadStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Component
class RetrospectiveAttachmentCleanup(
    private val attachmentRepository: RetrospectiveAttachmentRepository,
    private val storage: AttachmentStorage,
    private val transactionTemplate: TransactionTemplate,
) {
    fun cleanupExpired() {
        val expired = attachmentRepository.findAllByChatMessageIdIsNullAndExpiresAtBeforeAndDeletedAtIsNull(LocalDateTime.now())
        val removedRetrospectiveAttachments = attachmentRepository.findAllForDeletedRetrospectives()
        val pendingStorageDeletions = attachmentRepository.findAllByUploadStatus(AttachmentUploadStatus.DELETED)
        cleanup(expired, allowBoundAttachment = false)
        cleanup(removedRetrospectiveAttachments, allowBoundAttachment = true)
        cleanup(pendingStorageDeletions, allowBoundAttachment = true)
    }

    private fun cleanup(
        candidates: List<com.didit.domain.retrospect.RetrospectiveAttachment>,
        allowBoundAttachment: Boolean,
    ) {
        candidates.forEach { candidate ->
            val storageKey =
                transactionTemplate.execute {
                    attachmentRepository.findById(candidate.id)?.let {
                        when {
                            it.uploadStatus == AttachmentUploadStatus.DELETED -> it.storageKey
                            it.deletedAt == null && (allowBoundAttachment || it.chatMessageId == null) -> {
                                if (it.chatMessageId == null) it.delete() else it.deleteWithRetrospective()
                                attachmentRepository.save(it)
                                it.storageKey
                            }
                            else -> null
                        }
                    }
                } ?: return@forEach
            runCatching { storage.delete(storageKey) }
                .onSuccess {
                    transactionTemplate.executeWithoutResult {
                        attachmentRepository.findById(candidate.id)?.takeIf { it.deletedAt != null }?.also(attachmentRepository::delete)
                    }
                }.onFailure { exception ->
                    logger.warn("첨부파일 S3 삭제 실패 - attachmentId: ${candidate.id}", exception)
                }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectiveAttachmentCleanup::class.java)
    }
}
