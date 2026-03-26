package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.notification.dto.NotificationHistoryResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/notification-histories")
@RestController
class NotificationHistoryApi(
    private val notificationHistoryFinder: NotificationHistoryFinder,
    private val notificationHistoryRegister: NotificationHistoryRegister,
) {
    @RequireAuth
    @GetMapping
    fun findAll(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<NotificationHistoryResponse>> {
        val histories = notificationHistoryFinder.findAllByUserId(userId)
        return SuccessResponse.of(histories.map { NotificationHistoryResponse.from(it) })
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/read")
    fun readAll(
        @CurrentUserId userId: UUID,
    ) {
        notificationHistoryRegister.readAll(userId)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{notificationId}/read")
    fun read(
        @CurrentUserId userId: UUID,
        @PathVariable notificationId: UUID,
    ) {
        notificationHistoryRegister.read(notificationId, userId)
    }
}
