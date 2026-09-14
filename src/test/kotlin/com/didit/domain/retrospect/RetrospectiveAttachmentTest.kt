package com.didit.domain.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class RetrospectiveAttachmentTest {
    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @Test
    fun `지원 파일은 업로드 대기 상태로 생성한다`() {
        val attachment = attachment()

        assertThat(attachment.uploadStatus).isEqualTo(AttachmentUploadStatus.PENDING)
        assertThat(attachment.analysisStatus).isEqualTo(AttachmentAnalysisStatus.NOT_STARTED)
        assertThat(attachment.storageKey).startsWith("retrospectives/$userId/$retrospectiveId/")
    }

    @Test
    fun `10MB를 초과한 파일은 생성할 수 없다`() {
        assertThrows<IllegalArgumentException> {
            attachment(size = RetrospectiveAttachment.MAX_FILE_SIZE_BYTES + 1)
        }
    }

    @Test
    fun `업로드 완료 시 실제 크기와 MIME이 요청과 다르면 거절한다`() {
        val attachment = attachment()

        assertThrows<IllegalArgumentException> {
            attachment.completeUpload(10L, "application/pdf")
        }

        assertThat(attachment.uploadStatus).isEqualTo(AttachmentUploadStatus.PENDING)
    }

    @Test
    fun `업로드 완료 파일만 메시지에 연결하고 분석 상태를 전이한다`() {
        val attachment = attachment()
        val messageId = UUID.randomUUID()
        attachment.completeUpload(100L, "text/plain")

        attachment.bindTo(messageId)
        attachment.startAnalysis()
        attachment.completeAnalysis("배포 체크리스트를 작성했다.")

        assertThat(attachment.chatMessageId).isEqualTo(messageId)
        assertThat(attachment.analysisStatus).isEqualTo(AttachmentAnalysisStatus.COMPLETED)
        assertThat(attachment.extractedContent).isEqualTo("배포 체크리스트를 작성했다.")
    }

    @Test
    fun `실패한 분석은 재시도할 수 있다`() {
        val attachment = uploadedAttachment()
        attachment.bindTo(UUID.randomUUID())
        attachment.startAnalysis()
        attachment.failAnalysis("TEMPORARY_FAILURE")

        attachment.retryAnalysis()

        assertThat(attachment.analysisStatus).isEqualTo(AttachmentAnalysisStatus.PROCESSING)
        assertThat(attachment.analysisAttemptCount).isEqualTo(2)
        assertThat(attachment.analysisErrorCode).isNull()
    }

    @Test
    fun `메시지에 연결된 파일은 삭제할 수 없다`() {
        val attachment = uploadedAttachment()
        attachment.bindTo(UUID.randomUUID())

        assertThrows<IllegalStateException> {
            attachment.delete(LocalDateTime.now())
        }
    }

    private fun uploadedAttachment(): RetrospectiveAttachment = attachment().also { it.completeUpload(100L, "text/plain") }

    private fun attachment(
        size: Long = 100L,
        contentType: String = "text/plain",
    ): RetrospectiveAttachment =
        RetrospectiveAttachment.create(
            userId = userId,
            retrospectiveId = retrospectiveId,
            originalFilename = "work.md",
            contentType = contentType,
            expectedSize = size,
            expiresAt = LocalDateTime.now().plusHours(1),
        )
}
