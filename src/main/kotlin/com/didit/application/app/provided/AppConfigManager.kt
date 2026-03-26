package com.didit.application.app.provided

import com.didit.domain.app.AppConfig
import com.didit.domain.app.AppConfigUpdateRequest

interface AppConfigManager {
    fun getAppConfig(): AppConfig

    fun updateAppConfig(request: AppConfigUpdateRequest): AppConfig
}
