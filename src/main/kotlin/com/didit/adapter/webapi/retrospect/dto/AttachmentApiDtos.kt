package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.AttachmentDownloadResult
import com.didit.application.retrospect.dto.AttachmentResult
import com.didit.application.retrospect.dto.CreateAttachmentUploadResult
import com.didit.domain.retrospect.AttachmentAnalysisStatus
import com.didit.domain.retrospect.AttachmentFileType
import com.didit.domain.retrospect.AttachmentUploadStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime
import java.util.UUID

data class CreateAttachmentUploadRequest(
    @field:NotBlank
    val filename: String,
    @field:NotBlank
    val contentType: String,
    @field:Positive
    @field:Max(10L * 1024 * 1024)
    val size: Long,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z0-9+/]{43}=$")
    val checksumSha256: String?,
)

data class CreateAttachmentUploadResponse(
    val attachmentId: UUID,
    val uploadUrl: String,
    val expiresAt: LocalDateTime,
) {
    companion object {
        fun from(result: CreateAttachmentUploadResult) =
            CreateAttachmentUploadResponse(result.attachmentId, result.uploadUrl, result.expiresAt)
    }
}

data class AttachmentResponse(
    val id: UUID,
    val filename: String,
    val fileType: AttachmentFileType,
    val contentType: String,
    val size: Long,
    val uploadStatus: AttachmentUploadStatus,
    val analysisStatus: AttachmentAnalysisStatus,
) {
    companion object {
        fun from(result: AttachmentResult) =
            AttachmentResponse(
                result.id,
                result.originalFilename,
                result.fileType,
                result.contentType,
                result.size,
                result.uploadStatus,
                result.analysisStatus,
            )
    }
}

data class AttachmentDownloadResponse(
    val url: String,
    val expiresAt: LocalDateTime,
) {
    companion object {
        fun from(result: AttachmentDownloadResult) = AttachmentDownloadResponse(result.url, result.expiresAt)
    }
}
