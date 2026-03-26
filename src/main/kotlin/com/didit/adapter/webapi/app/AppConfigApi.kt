package com.didit.adapter.webapi.app

import com.didit.adapter.webapi.admin.annotation.RequireSuperAdmin
import com.didit.adapter.webapi.app.dto.AppConfigResponse
import com.didit.adapter.webapi.app.dto.AppConfigUpdateRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.app.provided.AppConfigManager
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1")
@RestController
class AppConfigApi(
    private val appConfigManager: AppConfigManager,
) {
    @GetMapping("/app/config")
    fun getAppConfig(): SuccessResponse<AppConfigResponse> = SuccessResponse.of(AppConfigResponse.from(appConfigManager.getAppConfig()))

    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/admin/settings/app-config")
    fun updateAppConfig(
        @Valid @RequestBody request: AppConfigUpdateRequest,
    ) {
        appConfigManager.updateAppConfig(request.toDomain())
    }
}
