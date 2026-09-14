package com.didit.application.retrospect

import com.didit.adapter.config.JpaAuditingConfig
import com.didit.application.retrospect.required.AttachmentStorage
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.application.retrospect.required.RetrospectiveConversationTurnRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.StoredAttachmentMetadata
import com.didit.domain.retrospect.AttachmentAnalysisStatus
import com.didit.domain.retrospect.AttachmentUploadStatus
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveAttachment
import com.didit.domain.retrospect.RetrospectiveConversationTurn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig::class, RetrospectiveAttachmentService::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RetrospectiveAttachmentServiceTest {
    @Autowired
    private lateinit var service: RetrospectiveAttachmentService

    @Autowired
    private lateinit var retrospectiveRepository: RetrospectiveRepository

    @Autowired
    private lateinit var attachmentRepository: RetrospectiveAttachmentRepository

    @Autowired
    private lateinit var messageRepository: ChatMessageRepository

    @Autowired
    private lateinit var turnRepository: RetrospectiveConversationTurnRepository

    @MockitoBean
    private lateinit var storage: AttachmentStorage

    private val userId = UUID.randomUUID()

    @Test
    fun `업로드 요청을 만들면 소유자 전용 저장 키와 presigned URL을 반환한다`() {
        val retrospective = savedRetrospective()
        whenever(storage.createUploadUrl(any(), any(), any(), any(), any())).thenReturn(URI("https://upload.example/file"))

        val result = service.createUpload(userId, retrospective.id, "work.md", "text/plain", 100L, CHECKSUM)

        val saved = attachmentRepository.findByIdAndUserId(result.attachmentId, userId)!!
        assertThat(result.uploadUrl).isEqualTo("https://upload.example/file")
        assertThat(saved.storageKey).contains("/$userId/${retrospective.id}/")
        assertThat(saved.uploadStatus).isEqualTo(AttachmentUploadStatus.PENDING)
    }

    @Test
    fun `다른 사용자의 회고에는 업로드 요청을 만들 수 없다`() {
        val retrospective = savedRetrospective()

        assertThrows<RuntimeException> {
            service.createUpload(UUID.randomUUID(), retrospective.id, "work.md", "text/plain", 100L, CHECKSUM)
        }

        verify(storage, never()).createUploadUrl(any(), any(), any(), any(), any())
    }

    @Test
    fun `S3 객체의 크기와 형식을 확인한 뒤 업로드 완료 처리한다`() {
        val retrospective = savedRetrospective()
        whenever(storage.createUploadUrl(any(), any(), any(), any(), any())).thenReturn(URI("https://upload.example/file"))
        val created = service.createUpload(userId, retrospective.id, "work.md", "text/plain", 100L, CHECKSUM)
        whenever(storage.inspect(any())).thenReturn(
            StoredAttachmentMetadata(
                contentLength = 100L,
                contentType = "text/plain",
                firstBytes = "업무 기록".toByteArray(),
            ),
        )

        val completed = service.completeUpload(userId, retrospective.id, created.attachmentId)

        assertThat(completed.uploadStatus).isEqualTo(AttachmentUploadStatus.UPLOADED)
    }

    @Test
    fun `업로드 완료 요청은 같은 결과를 멱등하게 반환한다`() {
        val retrospective = savedRetrospective()
        whenever(storage.createUploadUrl(any(), any(), any(), any(), any())).thenReturn(URI("https://upload.example/file"))
        val created = service.createUpload(userId, retrospective.id, "work.md", "text/plain", 100L, CHECKSUM)
        whenever(storage.inspect(any())).thenReturn(
            StoredAttachmentMetadata(100L, "text/plain", "업무 기록".toByteArray()),
        )

        val first = service.completeUpload(userId, retrospective.id, created.attachmentId)
        val duplicate = service.completeUpload(userId, retrospective.id, created.attachmentId)

        assertThat(duplicate).isEqualTo(first)
        verify(storage, org.mockito.kotlin.times(1)).inspect(any())
    }

    @Test
    fun `연결 전 첨부파일 삭제는 저장소 객체도 제거한다`() {
        val retrospective = savedRetrospective()
        whenever(storage.createUploadUrl(any(), any(), any(), any(), any())).thenReturn(URI("https://upload.example/file"))
        val created = service.createUpload(userId, retrospective.id, "work.md", "text/plain", 100L, CHECKSUM)

        service.delete(userId, retrospective.id, created.attachmentId)

        verify(storage).delete(any())
        assertThat(attachmentRepository.findByIdAndUserId(created.attachmentId, userId)).isNull()
    }

    @Test
    fun `민감정보가 감지된 첨부파일은 메시지 연결 후에도 완전히 삭제할 수 있다`() {
        val retrospective = savedRetrospective()
        val message = ChatMessage.v2UserMessageWithAttachments(retrospective, "첨부 확인", InputType.TEXT)
        retrospective.addMessage(message)
        retrospectiveRepository.save(retrospective)
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(userId, retrospective.id, "work.md", "text/plain", 3, LocalDateTime.now().plusHours(1))
                    .also {
                        it.completeUpload(3, "text/plain")
                        it.bindTo(message.id)
                        it.startAnalysis()
                        it.completeAnalysis("담당자 이메일은 [민감정보] 입니다.", containsSensitiveData = true)
                    },
            )

        service.delete(userId, retrospective.id, attachment.id)

        verify(storage).delete(attachment.storageKey)
        assertThat(attachmentRepository.findById(attachment.id)).isNull()
    }

    @Test
    fun `민감정보가 없는 첨부파일은 메시지 연결 후 삭제할 수 없다`() {
        val retrospective = savedRetrospective()
        val message = ChatMessage.v2UserMessageWithAttachments(retrospective, "첨부 확인", InputType.TEXT)
        retrospective.addMessage(message)
        retrospectiveRepository.save(retrospective)
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(userId, retrospective.id, "work.md", "text/plain", 3, LocalDateTime.now().plusHours(1))
                    .also {
                        it.completeUpload(3, "text/plain")
                        it.bindTo(message.id)
                    },
            )

        assertThrows<com.didit.application.retrospect.exception.AttachmentAlreadyBoundException> {
            service.delete(userId, retrospective.id, attachment.id)
        }

        verify(storage, never()).delete(any())
        assertThat(attachmentRepository.findById(attachment.id)).isNotNull()
    }

    @Test
    fun `분석 재시도는 워커가 파일 상태를 선점하도록 실패 상태로 이벤트를 발행한다`() {
        val retrospective = savedRetrospective()
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "첨부 확인", InputType.TEXT))
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(userId, retrospective.id, "work.md", "text/plain", 3, LocalDateTime.now().plusHours(1))
                    .also {
                        it.completeUpload(3, "text/plain")
                        it.bindTo(message.id)
                        it.startAnalysis()
                        it.failAnalysis("TEMPORARY_ANALYSIS_FAILURE")
                    },
            )
        turnRepository.save(
            RetrospectiveConversationTurn(
                retrospectiveId = retrospective.id,
                clientMessageId = UUID.randomUUID(),
                userMessageId = message.id,
                turnNumber = 1,
            ).also { it.fail("ATTACHMENT_ANALYSIS_FAILED") },
        )

        val result = service.retryAnalysis(userId, retrospective.id, attachment.id)

        assertThat(result.analysisStatus).isEqualTo(AttachmentAnalysisStatus.PROCESSING)
        assertThat(attachmentRepository.findById(attachment.id)!!.analysisStatus).isEqualTo(AttachmentAnalysisStatus.FAILED_RETRYABLE)
        assertThat(turnRepository.findByUserMessageId(message.id)!!.status)
            .isEqualTo(com.didit.domain.retrospect.ConversationTurnStatus.PROCESSING)
    }

    private fun savedRetrospective(): Retrospective =
        retrospectiveRepository.save(Retrospective.createV2(userId)).also {
            check(it.createdAt == null || it.createdAt!!.isBefore(LocalDateTime.now().plusSeconds(1)))
        }

    companion object {
        private const val CHECKSUM = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
}
