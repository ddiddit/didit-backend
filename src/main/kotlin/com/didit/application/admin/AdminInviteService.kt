package com.didit.application.admin

import com.didit.application.admin.exception.DuplicateAdminEmailException
import com.didit.application.admin.exception.DuplicateAdminInviteException
import com.didit.application.admin.exception.InvalidAdminInviteTokenException
import com.didit.application.admin.provided.AdminInviteManager
import com.didit.application.admin.required.AdminInviteRepository
import com.didit.application.admin.required.AdminRepository
import com.didit.application.admin.required.PasswordEncryptor
import com.didit.application.notification.required.EmailSender
import com.didit.domain.admin.Admin
import com.didit.domain.admin.AdminInvite
import com.didit.domain.admin.AdminInviteCreateRequest
import com.didit.domain.admin.AdminPosition
import com.didit.domain.admin.AdminRegisterRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminInviteService(
    private val adminRepository: AdminRepository,
    private val adminInviteRepository: AdminInviteRepository,
    private val emailSender: EmailSender,
    private val passwordEncryptor: PasswordEncryptor,
    @param:Value("\${admin.invite.expiry-hours:48}") private val expiryHours: Long,
    @param:Value("\${admin.invite.base-url}") private val baseUrl: String,
) : AdminInviteManager {
    companion object {
        private val logger = LoggerFactory.getLogger(AdminInviteService::class.java)
    }

    @Transactional
    override fun invite(
        invitedBy: UUID,
        email: String,
        position: AdminPosition,
    ) {
        if (adminRepository.findByEmail(email) != null) throw DuplicateAdminEmailException()
        if (adminInviteRepository.existsByEmailAndUsedAtIsNull(email)) throw DuplicateAdminInviteException()

        val invite =
            adminInviteRepository.save(
                AdminInvite.create(
                    AdminInviteCreateRequest(
                        email = email,
                        position = position,
                        invitedBy = invitedBy,
                        expiredAt = LocalDateTime.now().plusHours(expiryHours),
                    ),
                ),
            )

        emailSender.send(
            to = email,
            subject = "[didit] 관리자 초대",
            body = buildInviteEmailBody(invite.token),
        )

        logger.info("어드민 초대 이메일 발송 - invitedBy: $invitedBy, email: $email, position: $position")
    }

    @Transactional
    override fun register(
        token: UUID,
        email: String,
        password: String,
    ) {
        val invite = adminInviteRepository.findByToken(token) ?: throw InvalidAdminInviteTokenException()
        invite.use()

        adminInviteRepository.save(invite)

        adminRepository.save(
            Admin.register(
                AdminRegisterRequest(
                    email = email,
                    encodedPassword = passwordEncryptor.encode(password),
                    position = invite.position,
                ),
            ),
        )

        logger.info("어드민 등록 완료 - email: $email, position: ${invite.position}")
    }

    private fun buildInviteEmailBody(token: UUID): String {
        val template =
            ClassPathResource("templates/admin-invite.html")
                .inputStream
                .bufferedReader()
                .readText()

        return template
            .replace("{baseUrl}", baseUrl)
            .replace("{token}", token.toString())
            .replace("{expiryHours}", expiryHours.toString())
    }
}
