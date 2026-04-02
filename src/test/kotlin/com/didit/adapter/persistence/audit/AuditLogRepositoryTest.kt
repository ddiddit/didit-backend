package com.didit.adapter.persistence.audit

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class AuditLogRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var auditLogRepository: AuditLogRepository

    private val actorId = UUID.randomUUID()

    @Test
    fun `save - 감사 로그를 저장한다`() {
        val auditLog =
            AuditLog(
                actorId = actorId,
                actorType = ActorType.USER,
                action = AuditAction.RETROSPECTIVE_SAVED,
            )

        val saved = auditLogRepository.save(auditLog)

        assertThat(saved.actorId).isEqualTo(actorId)
        assertThat(saved.actorType).isEqualTo(ActorType.USER)
        assertThat(saved.action).isEqualTo(AuditAction.RETROSPECTIVE_SAVED)
    }

    @Test
    fun `save - targetId와 payload를 포함해 저장한다`() {
        val targetId = UUID.randomUUID()
        val auditLog =
            AuditLog(
                actorId = actorId,
                actorType = ActorType.USER,
                action = AuditAction.RETROSPECTIVE_DELETED,
                targetId = targetId,
                targetType = "RETROSPECTIVE",
                payload = mapOf("title" to "오늘의 회고"),
            )

        val saved = auditLogRepository.save(auditLog)

        assertThat(saved.targetId).isEqualTo(targetId)
        assertThat(saved.targetType).isEqualTo("RETROSPECTIVE")
        assertThat(saved.payload).containsEntry("title", "오늘의 회고")
    }
}
