package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.AttachmentDownloadResult
import com.didit.application.retrospect.dto.AttachmentResult
import com.didit.application.retrospect.dto.CreateAttachmentUploadResult
import java.util.UUID

interface RetrospectiveAttachments {
    fun createUpload(
        userId: UUID,
        retrospectiveId: UUID,
        originalFilename: String,
        contentType: String,
        size: Long,
        checksumSha256: String,
    ): CreateAttachmentUploadResult

    fun completeUpload(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ): AttachmentResult

    fun delete(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    )

    fun createDownload(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ): AttachmentDownloadResult

    fun retryAnalysis(
        userId: UUID,
        retrospectiveId: UUID,
        attachmentId: UUID,
    ): AttachmentResult
}
