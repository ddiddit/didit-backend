package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.AttachmentDownloadResponse
import com.didit.adapter.webapi.retrospect.dto.AttachmentResponse
import com.didit.adapter.webapi.retrospect.dto.CreateAttachmentUploadRequest
import com.didit.adapter.webapi.retrospect.dto.CreateAttachmentUploadResponse
import com.didit.application.retrospect.provided.RetrospectiveAttachments
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RetrospectiveAttachmentApi(
    private val attachments: RetrospectiveAttachments,
) {
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v2/retrospectives/{retrospectiveId}/attachments")
    fun createUpload(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @Valid @RequestBody request: CreateAttachmentUploadRequest,
    ): SuccessResponse<CreateAttachmentUploadResponse> =
        SuccessResponse.of(
            CreateAttachmentUploadResponse.from(
                attachments.createUpload(
                    userId,
                    retrospectiveId,
                    request.filename,
                    request.contentType,
                    request.size,
                    checkNotNull(request.checksumSha256),
                ),
            ),
        )

    @RequireAuth
    @PostMapping("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}/complete")
    fun completeUpload(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @PathVariable attachmentId: UUID,
    ): SuccessResponse<AttachmentResponse> =
        SuccessResponse.of(AttachmentResponse.from(attachments.completeUpload(userId, retrospectiveId, attachmentId)))

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @PathVariable attachmentId: UUID,
    ) = attachments.delete(userId, retrospectiveId, attachmentId)

    @RequireAuth
    @GetMapping("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}/download-url")
    fun createDownload(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @PathVariable attachmentId: UUID,
    ): SuccessResponse<AttachmentDownloadResponse> =
        SuccessResponse.of(AttachmentDownloadResponse.from(attachments.createDownload(userId, retrospectiveId, attachmentId)))

    @RequireAuth
    @PostMapping("/api/v2/retrospectives/{retrospectiveId}/attachments/{attachmentId}/retry-analysis")
    fun retryAnalysis(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @PathVariable attachmentId: UUID,
    ): SuccessResponse<AttachmentResponse> =
        SuccessResponse.of(AttachmentResponse.from(attachments.retryAnalysis(userId, retrospectiveId, attachmentId)))
}
