package com.didit.application.retrospect.dto

import com.didit.domain.retrospect.AttachmentAnalysisStatus
import com.didit.domain.retrospect.AttachmentFileType
import com.didit.domain.retrospect.AttachmentUploadStatus
import java.time.LocalDateTime
import java.util.UUID

data class CreateAttachmentUploadResult(
    val attachmentId: UUID,
    val uploadUrl: String,
    val expiresAt: LocalDateTime,
)

data class AttachmentResult(
    val id: UUID,
    val originalFilename: String,
    val fileType: AttachmentFileType,
    val contentType: String,
    val size: Long,
    val uploadStatus: AttachmentUploadStatus,
    val analysisStatus: AttachmentAnalysisStatus,
)

data class AttachmentDownloadResult(
    val url: String,
    val expiresAt: LocalDateTime,
)
