package com.didit.application.retrospect

import com.didit.application.retrospect.dto.AttachmentDownloadResult
import com.didit.application.retrospect.dto.AttachmentResult
import com.didit.application.retrospect.dto.CreateAttachmentUploadResult
import com.didit.application.retrospect.exception.AttachmentAlreadyBoundException
import com.didit.application.retrospect.exception.AttachmentInvalidFileException
import com.didit.application.retrospect.exception.AttachmentNotFoundException
import com.didit.application.retrospect.exception.AttachmentNotUploadedException
import com.didit.application.retrospect.exception.AttachmentSizeExceededException
import com.didit.application.retrospect.exception.AttachmentUnsupportedFileException
import com.didit.application.retrospect.exception.ConversationAlreadyFinishedException
import com.didit.application.retrospect.exception.RetrospectiveFlowVersionMismatchException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.provided.RetrospectiveAttachments
import com.didit.application.retrospect.required.AttachmentStorage
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.application.retrospect.required.RetrospectiveConversationTurnRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.AttachmentFileType
import com.didit.domain.retrospect.RetrospectiveAttachment
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class RetrospectiveAttachmentService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val attachmentRepository: RetrospectiveAttachmentRepository,
    private val storage: AttachmentStorage,
    private val transactionTemplate: TransactionTemplate,
    private val turnRepository: RetrospectiveConversationTurnRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : RetrospectiveAttachments {
    override fun createUpload(
        userId: UUID,
        retrospectiveId: UUID,
        originalFilename: String,
        contentType: String,
        size: Long,
        checksumSha256: String,
    ): CreateAttachmentUploadResult {
        val expiresAt = LocalDateTime.now().plus(UPLOAD_VALIDITY)
        val attachment =
            transactionTemplate.execute {
                findActiveV2(retrospectiveId, userId)
                try {
                    attachmentRepository.save(
                        RetrospectiveAttachment.create(userId, retrospectiveId, originalFilename, contentType, size, expiresAt),
                    )
                } catch (exception: IllegalArgumentException) {
                    if (size > RetrospectiveAttachment.MAX_FILE_SIZE_BYTES) throw AttachmentSizeExceededException()
                    throw AttachmentUnsupportedFileException()
                }
            }!!

        val uploadUrl =
            storage.createUploadUrl(
                attachment.storageKey,
                attachment.contentType,
                attachment.expectedSize,
                checksumSha256,
                UPLOAD_VALIDITY,
            )
        return CreateAttachmentUploadResult(attachment.id, uploadUrl.toString(), expiresAt)
    }

    override fun completeUpload(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ): AttachmentResult {
        val attachment = findOwned(attachmentId, userId, retrospectiveId)
        if (attachment.uploadStatus == com.didit.domain.retrospect.AttachmentUploadStatus.UPLOADED) return attachment.toResult()
        val metadata = storage.inspect(attachment.storageKey)
        if (!matchesSignature(attachment.fileType, metadata.firstBytes)) throw AttachmentInvalidFileException(attachmentId)

        return transactionTemplate.execute {
            val current = findOwned(attachmentId, userId, retrospectiveId)
            try {
                current.completeUpload(metadata.contentLength, metadata.contentType)
            } catch (exception: IllegalArgumentException) {
                throw AttachmentInvalidFileException(attachmentId)
            }
            attachmentRepository.save(current).toResult()
        }!!
    }

    override fun delete(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ) {
        val storageKey =
            transactionTemplate.execute {
                val current = findOwnedForUpdate(attachmentId, userId, retrospectiveId)
                if (current.chatMessageId != null && !current.containsSensitiveData) {
                    throw AttachmentAlreadyBoundException(attachmentId)
                }
                current.deleteByOwner()
                attachmentRepository.save(current)
                current.storageKey
            }!!

        storage.delete(storageKey)
        transactionTemplate.executeWithoutResult {
            attachmentRepository.findById(attachmentId)?.takeIf { it.deletedAt != null }?.also(attachmentRepository::delete)
        }
    }

    override fun createDownload(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ): AttachmentDownloadResult {
        val attachment = findOwned(attachmentId, userId, retrospectiveId)
        if (attachment.uploadStatus != com.didit.domain.retrospect.AttachmentUploadStatus.UPLOADED) {
            throw AttachmentNotUploadedException(attachmentId)
        }
        val expiresAt = LocalDateTime.now().plus(DOWNLOAD_VALIDITY)
        return AttachmentDownloadResult(
            storage.createDownloadUrl(attachment.storageKey, DOWNLOAD_VALIDITY).toString(),
            expiresAt,
        )
    }

    override fun retryAnalysis(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ): AttachmentResult {
        val retry =
            transactionTemplate.execute {
                val attachment = findOwned(attachmentId, userId, retrospectiveId)
                val messageId = attachment.chatMessageId ?: throw AttachmentInvalidFileException(attachmentId)
                val turn = turnRepository.findByUserMessageId(messageId) ?: throw AttachmentInvalidFileException(attachmentId)
                if (turn.status != com.didit.domain.retrospect.ConversationTurnStatus.FAILED) {
                    throw AttachmentInvalidFileException(attachmentId)
                }
                if (attachment.analysisStatus == com.didit.domain.retrospect.AttachmentAnalysisStatus.UNREADABLE) {
                    throw AttachmentInvalidFileException(attachmentId)
                }
                turn.retry()
                turnRepository.save(turn)
                RetryResult(
                    AttachmentConversationRequestedEvent(retrospectiveId, userId, turn.id, messageId),
                    attachment.toResult().copy(
                        analysisStatus =
                            if (attachment.analysisStatus ==
                                com.didit.domain.retrospect.AttachmentAnalysisStatus.FAILED_RETRYABLE
                            ) {
                                com.didit.domain.retrospect.AttachmentAnalysisStatus.PROCESSING
                            } else {
                                attachment.analysisStatus
                            },
                    ),
                )
            }!!
        eventPublisher.publishEvent(retry.event)
        return retry.result
    }

    private fun findOwned(
        attachmentId: UUID,
        userId: UUID,
        retrospectiveId: UUID,
    ): RetrospectiveAttachment {
        val retrospective =
            retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isV2()) throw RetrospectiveFlowVersionMismatchException(retrospectiveId)
        return attachmentRepository
            .findByIdAndUserId(
                attachmentId,
                userId,
            )?.takeIf { it.retrospectiveId == retrospectiveId && it.deletedAt == null }
            ?: throw AttachmentNotFoundException(attachmentId)
    }

    private fun findOwnedForUpdate(
        attachmentId: UUID,
        userId: UUID,
        retrospectiveId: UUID,
    ): RetrospectiveAttachment {
        val retrospective =
            retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isV2()) throw RetrospectiveFlowVersionMismatchException(retrospectiveId)
        return attachmentRepository
            .findByIdAndUserIdForUpdate(attachmentId, userId)
            ?.takeIf { it.retrospectiveId == retrospectiveId && it.deletedAt == null }
            ?: throw AttachmentNotFoundException(attachmentId)
    }

    private fun findActiveV2(
        retrospectiveId: UUID,
        userId: UUID,
    ) = retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)?.also {
        if (!it.isV2()) throw RetrospectiveFlowVersionMismatchException(retrospectiveId)
        if (!it.isConversationActive()) throw ConversationAlreadyFinishedException(retrospectiveId)
    } ?: throw RetrospectiveNotFoundException(retrospectiveId)

    private fun matchesSignature(
        fileType: AttachmentFileType,
        bytes: ByteArray,
    ): Boolean =
        when (fileType) {
            AttachmentFileType.JPG -> bytes.startsWith(0xFF, 0xD8, 0xFF)
            AttachmentFileType.PNG -> bytes.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            AttachmentFileType.PDF -> bytes.decodeToString().startsWith("%PDF-")
            AttachmentFileType.TXT, AttachmentFileType.MD -> bytes.isNotEmpty() && bytes.none { it == 0.toByte() }
        }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
        size >= prefix.size && prefix.indices.all { this[it].toInt() and 0xFF == prefix[it] }

    private fun RetrospectiveAttachment.toResult() =
        AttachmentResult(id, originalFilename, fileType, contentType, actualSize ?: expectedSize, uploadStatus, analysisStatus)

    companion object {
        private val UPLOAD_VALIDITY = Duration.ofMinutes(5)
        private val DOWNLOAD_VALIDITY = Duration.ofMinutes(5)
    }

    private data class RetryResult(
        val event: AttachmentConversationRequestedEvent,
        val result: AttachmentResult,
    )
}
