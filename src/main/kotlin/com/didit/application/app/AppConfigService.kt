package com.didit.application.app

import com.didit.application.app.provided.AppConfigManager
import com.didit.application.app.required.AppConfigRepository
import com.didit.domain.app.AppConfig
import com.didit.domain.app.AppConfigUpdateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AppConfigService(
    private val appConfigRepository: AppConfigRepository,
) : AppConfigManager {
    override fun getAppConfig(): AppConfig = appConfigRepository.findFirst() ?: AppConfig()

    @Transactional
    override fun updateAppConfig(request: AppConfigUpdateRequest): AppConfig {
        val config = appConfigRepository.findFirst() ?: AppConfig()
        config.update(request)
        return appConfigRepository.save(config)
    }
}
