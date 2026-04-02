package com.didit.adapter.aspect

import com.didit.application.audit.ActorType
import com.didit.application.audit.Audit
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import org.aspectj.lang.ProceedingJoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuditAspectTest {
    @Mock
    lateinit var auditLogger: AuditLogger

    @Mock
    lateinit var joinPoint: ProceedingJoinPoint

    private lateinit var auditAspect: AuditAspect

    private val userId = UUID.randomUUID()
    private val targetId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        auditAspect = AuditAspect(auditLogger)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun setUserAuthentication(id: UUID = userId) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                id.toString(),
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
    }

    private fun setAdminAuthentication(id: UUID = userId) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                id.toString(),
                null,
                listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
            )
    }

    @Test
    fun `logAudit - 유저 인증 정보가 있으면 USER 타입으로 audit 로그를 저장한다`() {
        setUserAuthentication()
        val audit = Audit(action = AuditAction.RETROSPECTIVE_STARTED)
        whenever(joinPoint.args).thenReturn(arrayOf())
        whenever(joinPoint.proceed()).thenReturn(null)

        auditAspect.logAudit(joinPoint, audit)

        verify(auditLogger).log(
            actorId = userId,
            actorType = ActorType.USER,
            action = AuditAction.RETROSPECTIVE_STARTED,
            targetId = null,
            targetType = null,
        )
    }

    @Test
    fun `logAudit - 어드민 인증 정보가 있으면 ADMIN 타입으로 audit 로그를 저장한다`() {
        setAdminAuthentication()
        val audit = Audit(action = AuditAction.ADMIN_LOGGED_OUT)
        whenever(joinPoint.args).thenReturn(arrayOf())
        whenever(joinPoint.proceed()).thenReturn(null)

        auditAspect.logAudit(joinPoint, audit)

        verify(auditLogger).log(
            actorId = userId,
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_LOGGED_OUT,
            targetId = null,
            targetType = null,
        )
    }

    @Test
    fun `logAudit - targetType이 있으면 targetId도 함께 저장한다`() {
        setUserAuthentication()
        val audit = Audit(action = AuditAction.RETROSPECTIVE_SAVED, targetType = "RETROSPECTIVE")
        whenever(joinPoint.args).thenReturn(arrayOf(userId, targetId))
        whenever(joinPoint.proceed()).thenReturn(null)

        auditAspect.logAudit(joinPoint, audit)

        verify(auditLogger).log(
            actorId = userId,
            actorType = ActorType.USER,
            action = AuditAction.RETROSPECTIVE_SAVED,
            targetId = targetId,
            targetType = "RETROSPECTIVE",
        )
    }

    @Test
    fun `logAudit - 인증 정보가 없으면 audit 로그를 저장하지 않는다`() {
        val audit = Audit(action = AuditAction.RETROSPECTIVE_STARTED)
        whenever(joinPoint.proceed()).thenReturn(null)

        auditAspect.logAudit(joinPoint, audit)

        verify(auditLogger, never()).log(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `logAudit - audit 로그 저장 실패해도 예외가 전파되지 않는다`() {
        setUserAuthentication()
        val audit = Audit(action = AuditAction.RETROSPECTIVE_STARTED)
        whenever(joinPoint.args).thenReturn(arrayOf())
        whenever(joinPoint.proceed()).thenReturn(null)
        whenever(auditLogger.log(any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("저장 실패"))

        assertDoesNotThrow {
            auditAspect.logAudit(joinPoint, audit)
        }
    }

    @Test
    fun `logAudit - 원래 메서드 결과를 반환한다`() {
        setUserAuthentication()
        val audit = Audit(action = AuditAction.RETROSPECTIVE_STARTED)
        whenever(joinPoint.args).thenReturn(arrayOf())
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = auditAspect.logAudit(joinPoint, audit)

        assertThat(result).isEqualTo("result")
    }
}
