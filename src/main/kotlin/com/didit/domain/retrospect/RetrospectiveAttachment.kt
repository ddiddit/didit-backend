package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

enum class AttachmentFileType(
    val extensions: Set<String>,
    val contentTypes: Set<String>,
) {
    JPG(setOf("jpg", "jpeg"), setOf("image/jpeg")),
    PNG(setOf("png"), setOf("image/png")),
    PDF(setOf("pdf"), setOf("application/pdf")),
    TXT(setOf("txt"), setOf("text/plain")),
    MD(setOf("md", "markdown"), setOf("text/markdown", "text/plain", "text/x-markdown")),
    ;

    companion object {
        fun resolve(
            filename: String,
            contentType: String,
        ): AttachmentFileType {
            val extension = filename.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return entries.firstOrNull { extension in it.extensions && contentType.lowercase() in it.contentTypes }
                ?: throw IllegalArgumentException("지원하지 않는 파일 형식입니다.")
        }
    }
}

enum class AttachmentUploadStatus {
    PENDING,
    UPLOADED,
    FAILED,
    DELETED,
}

enum class AttachmentAnalysisStatus {
    NOT_STARTED,
    PROCESSING,
    COMPLETED,
    FAILED_RETRYABLE,
    UNREADABLE,
}

@Table(name = "retrospective_attachments")
@Entity
class RetrospectiveAttachment(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val retrospectiveId: UUID,
    @Column(columnDefinition = "BINARY(16)")
    var chatMessageId: UUID? = null,
    @Column(nullable = false, length = 500)
    val storageKey: String,
    @Column(nullable = false, length = 255)
    val originalFilename: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val fileType: AttachmentFileType,
    @Column(nullable = false, length = 100)
    val contentType: String,
    @Column(nullable = false)
    val expectedSize: Long,
    @Column
    var actualSize: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var uploadStatus: AttachmentUploadStatus = AttachmentUploadStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var analysisStatus: AttachmentAnalysisStatus = AttachmentAnalysisStatus.NOT_STARTED,
    @Column(nullable = false)
    var analysisAttemptCount: Int = 0,
    @Column(length = 50)
    var analysisErrorCode: String? = null,
    @Column(columnDefinition = "MEDIUMTEXT")
    var extractedContent: String? = null,
    @Column(nullable = false)
    var containsSensitiveData: Boolean = false,
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
    @Column
    var uploadedAt: LocalDateTime? = null,
    @Column
    var analyzedAt: LocalDateTime? = null,
    @Column
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun completeUpload(
        actualSize: Long,
        actualContentType: String,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        check(uploadStatus == AttachmentUploadStatus.PENDING) { "업로드 대기 상태가 아닙니다." }
        require(actualSize == expectedSize && actualSize in 1..MAX_FILE_SIZE_BYTES) { "파일 크기가 요청과 일치하지 않습니다." }
        require(actualContentType.substringBefore(';').trim().lowercase() in fileType.contentTypes) {
            "파일 형식이 요청과 일치하지 않습니다."
        }
        this.actualSize = actualSize
        this.uploadStatus = AttachmentUploadStatus.UPLOADED
        this.uploadedAt = now
    }

    fun bindTo(messageId: UUID) {
        check(uploadStatus == AttachmentUploadStatus.UPLOADED) { "업로드가 완료된 파일만 메시지에 연결할 수 있습니다." }
        check(chatMessageId == null || chatMessageId == messageId) { "이미 다른 메시지에 연결된 파일입니다." }
        chatMessageId = messageId
    }

    fun startAnalysis() {
        check(chatMessageId != null) { "메시지에 연결된 파일만 분석할 수 있습니다." }
        check(analysisStatus == AttachmentAnalysisStatus.NOT_STARTED) { "분석을 시작할 수 없는 상태입니다." }
        analysisStatus = AttachmentAnalysisStatus.PROCESSING
        analysisAttemptCount = 1
        analysisErrorCode = null
    }

    fun completeAnalysis(
        content: String,
        containsSensitiveData: Boolean = false,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        check(analysisStatus == AttachmentAnalysisStatus.PROCESSING) { "분석 중인 파일이 아닙니다." }
        require(content.isNotBlank()) { "분석 결과는 비어 있을 수 없습니다." }
        extractedContent = content.trim()
        this.containsSensitiveData = containsSensitiveData
        analysisStatus = AttachmentAnalysisStatus.COMPLETED
        analysisErrorCode = null
        analyzedAt = now
    }

    fun failAnalysis(errorCode: String) {
        check(analysisStatus == AttachmentAnalysisStatus.PROCESSING) { "분석 중인 파일이 아닙니다." }
        analysisStatus = AttachmentAnalysisStatus.FAILED_RETRYABLE
        analysisErrorCode = errorCode
    }

    fun markUnreadable(errorCode: String) {
        check(analysisStatus == AttachmentAnalysisStatus.PROCESSING) { "분석 중인 파일이 아닙니다." }
        analysisStatus = AttachmentAnalysisStatus.UNREADABLE
        analysisErrorCode = errorCode
    }

    fun retryAnalysis() {
        check(analysisStatus == AttachmentAnalysisStatus.FAILED_RETRYABLE) { "재시도 가능한 상태가 아닙니다." }
        analysisStatus = AttachmentAnalysisStatus.PROCESSING
        analysisAttemptCount += 1
        analysisErrorCode = null
    }

    fun restartStalledAnalysis() {
        check(analysisStatus == AttachmentAnalysisStatus.PROCESSING) { "분석 중인 파일이 아닙니다." }
        analysisAttemptCount += 1
        analysisErrorCode = null
    }

    fun deleteByOwner(now: LocalDateTime = LocalDateTime.now()) {
        check(chatMessageId == null || containsSensitiveData) { "메시지에 연결된 파일은 삭제할 수 없습니다." }
        markDeletedAndPurge(now)
    }

    fun delete(now: LocalDateTime = LocalDateTime.now()) {
        check(chatMessageId == null) { "메시지에 연결된 파일은 삭제할 수 없습니다." }
        markDeletedAndPurge(now)
    }

    private fun markDeletedAndPurge(now: LocalDateTime) {
        uploadStatus = AttachmentUploadStatus.DELETED
        extractedContent = null
        containsSensitiveData = false
        deletedAt = now
    }

    fun deleteWithRetrospective(now: LocalDateTime = LocalDateTime.now()) {
        markDeletedAndPurge(now)
    }

    companion object {
        const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024

        fun create(
            userId: UUID,
            retrospectiveId: UUID,
            originalFilename: String,
            contentType: String,
            expectedSize: Long,
            expiresAt: LocalDateTime,
        ): RetrospectiveAttachment {
            require(expectedSize in 1..MAX_FILE_SIZE_BYTES) { "파일 크기는 10MB 이하여야 합니다." }
            require(originalFilename.isNotBlank() && originalFilename.length <= 255) { "파일명이 올바르지 않습니다." }
            val fileType = AttachmentFileType.resolve(originalFilename, contentType)
            val id = UUID.randomUUID()
            val safeExtension = originalFilename.substringAfterLast('.').lowercase()
            return RetrospectiveAttachment(
                id = id,
                userId = userId,
                retrospectiveId = retrospectiveId,
                storageKey = "retrospectives/$userId/$retrospectiveId/$id.$safeExtension",
                originalFilename = originalFilename,
                fileType = fileType,
                contentType = contentType.lowercase(),
                expectedSize = expectedSize,
                expiresAt = expiresAt,
            )
        }
    }
}
