package com.didit.application.app.required

import com.didit.domain.app.AppConfig
import org.springframework.data.repository.Repository
import java.util.UUID

interface AppConfigRepository : Repository<AppConfig, UUID> {
    fun findFirst(): AppConfig?

    fun save(appConfig: AppConfig): AppConfig
}
