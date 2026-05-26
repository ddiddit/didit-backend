package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireSuperAdmin
import com.didit.adapter.webapi.notification.dto.AdminNoticeEmailSendApiRequest
import com.didit.application.notification.provided.AdminNoticeEmailSender
import com.didit.domain.notification.AdminNoticeEmailSendRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/notice-emails")
@RestController
class AdminNoticeEmailApi(
    private val adminNoticeEmailSender: AdminNoticeEmailSender,
) {

    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    fun send(
        @CurrentAdminId adminId: UUID,
        @RequestBody request: AdminNoticeEmailSendApiRequest,
    ) {
        adminNoticeEmailSender.send(
            AdminNoticeEmailSendRequest(
                adminId = adminId,
                targetType = request.targetType,
                userIds = request.userIds,
                subject = request.subject,
                body = request.body,
            )
        )
    }
}