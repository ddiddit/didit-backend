package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.notification.dto.NotificationSettingResponse
import com.didit.adapter.webapi.notification.dto.UpdateConsentRequest
import com.didit.adapter.webapi.notification.dto.UpdateMarketingConsentRequest
import com.didit.adapter.webapi.notification.dto.UpdateNotificationSettingRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.NotificationSettingModifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/notification-settings")
@RestController
class NotificationSettingApi(
    private val notificationSettingFinder: NotificationSettingFinder,
    private val notificationSettingModifier: NotificationSettingModifier,
    private val userFinder: UserFinder,
    private val userRegister: UserRegister,
) {
    @RequireAuth
    @GetMapping
    fun findByUserId(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<NotificationSettingResponse> {
        val setting = notificationSettingFinder.findByUserId(userId)
        val user = userFinder.findByIdOrThrow(userId)
        return SuccessResponse.of(
            NotificationSettingResponse.of(
                setting = setting,
                marketingAgreed = user.marketingAgreed,
            ),
        )
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping
    fun updateSetting(
        @CurrentUserId userId: UUID,
        @RequestBody request: UpdateNotificationSettingRequest,
    ) {
        notificationSettingModifier.updateSetting(userId, request.enabled, request.reminderTime)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/night-push-consent")
    fun updateNightPushConsent(
        @CurrentUserId userId: UUID,
        @RequestBody request: UpdateConsentRequest,
    ) {
        notificationSettingModifier.updateNightPushConsent(userId, request.consent)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/marketing-consent")
    fun updateMarketingConsent(
        @CurrentUserId userId: UUID,
        @RequestBody request: UpdateMarketingConsentRequest,
    ) {
        userRegister.updateMarketingConsent(userId, request.agreed)
    }
}
