package com.didit.application.app

import com.didit.application.app.provided.AppConfigManager
import com.didit.application.app.required.AppConfigRepository
import com.didit.domain.app.AppConfig
import com.didit.domain.app.AppConfigUpdateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AppConfigService(
    private val appConfigRepository: AppConfigRepository,
) : AppConfigManager {
    companion object {
        private val logger = LoggerFactory.getLogger(AppConfigService::class.java)
    }

    override fun getAppConfig(): AppConfig = appConfigRepository.findFirstBy() ?: AppConfig()

    @Transactional
    override fun updateAppConfig(request: AppConfigUpdateRequest): AppConfig {
        val config = appConfigRepository.findFirstBy() ?: AppConfig()
        config.update(request)

        val saved = appConfigRepository.save(config)

        logger.info("앱 설정 수정 - maintenanceMode: ${request.maintenanceMode}, minimumVersion: ${request.minimumVersion}")

        return saved
    }
}
