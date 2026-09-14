package com.didit.application.retrospect

import com.didit.adapter.config.JpaAuditingConfig
import com.didit.application.retrospect.required.AttachmentContentAnalyzer
import com.didit.application.retrospect.required.AttachmentStorage
import com.didit.application.retrospect.required.ChatMessageRepository
import com.didit.application.retrospect.required.RetrospectiveAttachmentRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.AttachmentAnalysisStatus
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveAttachment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig::class, AttachmentAnalysisProcessor::class, AttachmentSensitiveDataDetector::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AttachmentAnalysisProcessorTest {
    @Autowired
    private lateinit var processor: AttachmentAnalysisProcessor

    @Autowired
    private lateinit var retrospectiveRepository: RetrospectiveRepository

    @Autowired
    private lateinit var messageRepository: ChatMessageRepository

    @Autowired
    private lateinit var attachmentRepository: RetrospectiveAttachmentRepository

    @MockitoBean
    private lateinit var storage: AttachmentStorage

    @MockitoBean
    private lateinit var analyzer: AttachmentContentAnalyzer

    @MockitoBean
    private lateinit var conversationService: RetrospectiveConversationV2Service

    @Test
    fun `파일을 읽고 분석한 뒤 회고 AI 처리를 이어간다`() {
        val userId = UUID.randomUUID()
        val retrospective = retrospectiveRepository.save(Retrospective.createV2(userId))
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "배포 자료", InputType.TEXT))
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(
                        userId,
                        retrospective.id,
                        "work.md",
                        "text/plain",
                        3,
                        LocalDateTime.now().plusHours(1),
                    ).also {
                        it.completeUpload(3, "text/plain")
                        it.bindTo(message.id)
                    },
            )
        whenever(storage.read(attachment.storageKey)).thenReturn("업무".toByteArray())
        whenever(analyzer.analyze(any(), any(), any())).thenReturn("담당자 이메일은 worker@example.com 입니다.")
        val event = AttachmentConversationRequestedEvent(retrospective.id, userId, UUID.randomUUID(), message.id)

        processor.process(event)

        val saved = attachmentRepository.findById(attachment.id)!!
        assertThat(saved.analysisStatus).isEqualTo(AttachmentAnalysisStatus.COMPLETED)
        assertThat(saved.extractedContent).isEqualTo("담당자 이메일은 [민감정보] 입니다.")
        assertThat(saved.containsSensitiveData).isTrue()
        verify(conversationService).processAttachedTurn(event)
    }

    @Test
    fun `읽을 수 없는 파일은 재시도 실패로 처리하지 않고 안내 응답을 완료한다`() {
        val userId = UUID.randomUUID()
        val retrospective = retrospectiveRepository.save(Retrospective.createV2(userId))
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "", InputType.TEXT))
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(
                        userId,
                        retrospective.id,
                        "broken.pdf",
                        "application/pdf",
                        3,
                        LocalDateTime.now().plusHours(1),
                    ).also {
                        it.completeUpload(3, "application/pdf")
                        it.bindTo(message.id)
                    },
            )
        whenever(storage.read(attachment.storageKey)).thenReturn(byteArrayOf(1, 2, 3))
        whenever(analyzer.analyze(any(), any(), any())).thenThrow(IllegalArgumentException("읽을 수 없음"))
        val event = AttachmentConversationRequestedEvent(retrospective.id, userId, UUID.randomUUID(), message.id)

        processor.process(event)

        assertThat(attachmentRepository.findById(attachment.id)!!.analysisStatus).isEqualTo(AttachmentAnalysisStatus.UNREADABLE)
        verify(conversationService).completeUnreadableAttachedTurn(event)
        verify(conversationService, never()).failAttachedTurn(any(), any())
        verify(conversationService, never()).processAttachedTurn(any())
    }

    @Test
    fun `사용자 텍스트가 있으면 손상 파일을 제외하고 회고 처리를 이어간다`() {
        val userId = UUID.randomUUID()
        val retrospective = retrospectiveRepository.save(Retrospective.createV2(userId))
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "배포를 완료했습니다.", InputType.TEXT))
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(
                        userId,
                        retrospective.id,
                        "broken.pdf",
                        "application/pdf",
                        3,
                        LocalDateTime.now().plusHours(1),
                    ).also {
                        it.completeUpload(3, "application/pdf")
                        it.bindTo(message.id)
                    },
            )
        whenever(storage.read(attachment.storageKey)).thenReturn(byteArrayOf(1, 2, 3))
        whenever(analyzer.analyze(any(), any(), any())).thenThrow(IllegalArgumentException("읽을 수 없음"))
        val event = AttachmentConversationRequestedEvent(retrospective.id, userId, UUID.randomUUID(), message.id)

        processor.process(event)

        verify(conversationService).processAttachedTurn(event)
        verify(conversationService, never()).completeUnreadableAttachedTurn(any())
    }

    @Test
    fun `복구 이벤트에서 이미 읽기 불가인 파일은 다시 분석하지 않고 안내 응답을 완료한다`() {
        val userId = UUID.randomUUID()
        val retrospective = retrospectiveRepository.save(Retrospective.createV2(userId))
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "", InputType.TEXT))
        attachmentRepository.save(
            RetrospectiveAttachment
                .create(
                    userId,
                    retrospective.id,
                    "broken.pdf",
                    "application/pdf",
                    3,
                    LocalDateTime.now().plusHours(1),
                ).also {
                    it.completeUpload(3, "application/pdf")
                    it.bindTo(message.id)
                    it.startAnalysis()
                    it.markUnreadable("UNREADABLE_FILE")
                },
        )
        val event = AttachmentConversationRequestedEvent(retrospective.id, userId, UUID.randomUUID(), message.id)

        processor.process(event)

        verify(storage, never()).read(any())
        verify(analyzer, never()).analyze(any(), any(), any())
        verify(conversationService).completeUnreadableAttachedTurn(event)
    }

    @Test
    fun `이미 다른 이벤트가 분석 중이면 중복 이벤트는 아무 작업도 하지 않는다`() {
        val userId = UUID.randomUUID()
        val retrospective = retrospectiveRepository.save(Retrospective.createV2(userId))
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "첨부 확인", InputType.TEXT))
        attachmentRepository.save(
            RetrospectiveAttachment
                .create(userId, retrospective.id, "work.md", "text/plain", 3, LocalDateTime.now().plusHours(1))
                .also {
                    it.completeUpload(3, "text/plain")
                    it.bindTo(message.id)
                    it.startAnalysis()
                },
        )
        val event = AttachmentConversationRequestedEvent(retrospective.id, userId, UUID.randomUUID(), message.id)

        processor.process(event)

        verify(storage, never()).read(any())
        verify(analyzer, never()).analyze(any(), any(), any())
        verify(conversationService, never()).processAttachedTurn(any())
        verify(conversationService, never()).completeUnreadableAttachedTurn(any())
        verify(conversationService, never()).failAttachedTurn(any(), any())
    }

    @Test
    fun `복구 이벤트는 멈춘 분석을 다시 시작한다`() {
        val userId = UUID.randomUUID()
        val retrospective = retrospectiveRepository.save(Retrospective.createV2(userId))
        val message = messageRepository.save(ChatMessage.v2UserMessageWithAttachments(retrospective, "첨부 확인", InputType.TEXT))
        val attachment =
            attachmentRepository.save(
                RetrospectiveAttachment
                    .create(userId, retrospective.id, "work.md", "text/plain", 3, LocalDateTime.now().plusHours(1))
                    .also {
                        it.completeUpload(3, "text/plain")
                        it.bindTo(message.id)
                        it.startAnalysis()
                    },
            )
        whenever(storage.read(attachment.storageKey)).thenReturn("업무".toByteArray())
        whenever(analyzer.analyze(any(), any(), any())).thenReturn("배포를 완료했습니다.")
        val event = AttachmentConversationRequestedEvent(retrospective.id, userId, UUID.randomUUID(), message.id, recovery = true)

        processor.process(event)

        assertThat(attachmentRepository.findById(attachment.id)!!.analysisStatus).isEqualTo(AttachmentAnalysisStatus.COMPLETED)
        verify(conversationService).processAttachedTurn(event)
    }
}
