package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireSuperAdmin
import com.didit.adapter.webapi.notification.dto.AdminNoticePushSendApiRequest
import com.didit.application.notification.provided.AdminNoticePushSender
import com.didit.domain.notification.AdminNoticePushSendRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/notice-pushes")
@RestController
class AdminNoticePushApi(
    private val adminNoticePushSender: AdminNoticePushSender,
) {
    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    fun send(
        @CurrentAdminId adminId: UUID,
        @RequestBody request: AdminNoticePushSendApiRequest,
    ) {
        adminNoticePushSender.send(
            AdminNoticePushSendRequest(
                adminId = adminId,
                targetType = request.targetType,
                userIds = request.userIds,
                title = request.title,
                body = request.body,
                link = request.link,
            ),
        )
    }
}
