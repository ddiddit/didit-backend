package com.didit.application.admin

import com.didit.application.audit.AdminAuditLogEntry
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditPageResult
import com.didit.application.audit.AuditReader
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AdminAuditServiceTest {
    @Mock
    lateinit var auditReader: AuditReader

    @InjectMocks
    lateinit var adminAuditService: AdminAuditService

    private fun createPageResult(vararg actions: AuditAction) = AuditPageResult(
        content = actions.map {
            AdminAuditLogEntry(
                action = it,
                actorId = null,
                actorType = "ADMIN",
                targetId = null,
                targetType = null,
                payload = null,
                createdAt = LocalDateTime.now(),
            )
        },
        totalElements = actions.size.toLong(),
        totalPages = 1,
        page = 0,
    )

    @Test
    fun `감사 로그 전체 조회`() {
        val pageResult = createPageResult(AuditAction.ADMIN_INVITED, AuditAction.ADMIN_APPROVED)
        whenever(auditReader.findAuditLogs(isNull(), isNull(), eq(0), eq(20))).thenReturn(pageResult)

        val result = adminAuditService.findAuditLogs(null, null, 0)

        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    fun `감사 로그 action 필터 조회`() {
        val pageResult = createPageResult(AuditAction.ADMIN_INVITED)
        whenever(auditReader.findAuditLogs(eq(AuditAction.ADMIN_INVITED), isNull(), eq(0), eq(20))).thenReturn(pageResult)

        val result = adminAuditService.findAuditLogs("ADMIN_INVITED", null, 0)

        assertThat(result.content[0].action).isEqualTo("ADMIN_INVITED")
    }

    @Test
    fun `감사 로그 유효하지 않은 action 필터 - 예외 발생`() {
        assertThatThrownBy { adminAuditService.findAuditLogs("INVALID_ACTION", null, 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 action 값")
    }
}
