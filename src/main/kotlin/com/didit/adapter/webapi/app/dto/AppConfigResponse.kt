package com.didit.adapter.webapi.app.dto

import com.didit.domain.app.AppConfig

data class AppConfigResponse(
    val maintenanceMode: Boolean,
    val maintenanceMessage: String?,
    val minimumVersion: String,
) {
    companion object {
        fun from(appConfig: AppConfig) =
            AppConfigResponse(
                maintenanceMode = appConfig.maintenanceMode,
                maintenanceMessage = appConfig.maintenanceMessage,
                minimumVersion = appConfig.minimumVersion,
            )
    }
}
