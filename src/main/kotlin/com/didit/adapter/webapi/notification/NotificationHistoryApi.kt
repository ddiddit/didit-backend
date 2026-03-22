package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.notification.dto.NotificationHistoryResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
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
    @GetMapping
    fun findAll(
        @RequestHeader("X-User-Id") userId: UUID,
    ): SuccessResponse<List<NotificationHistoryResponse>> {
        val histories = notificationHistoryFinder.findAllByUserId(userId)

        return SuccessResponse.of(histories.map { NotificationHistoryResponse.from(it) })
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/read")
    fun readAll(
        @RequestHeader("X-User-Id") userId: UUID,
    ) {
        notificationHistoryRegister.readAll(userId)
    }
}
