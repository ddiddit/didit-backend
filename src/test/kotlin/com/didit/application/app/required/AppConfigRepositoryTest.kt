package com.didit.application.app.required

import com.didit.domain.app.AppConfig
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AppConfigRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var appConfigRepository: AppConfigRepository

    @Test
    fun `save`() {
        val config = AppConfig()

        val saved = appConfigRepository.save(config)

        assertThat(saved.maintenanceMode).isFalse()
        assertThat(saved.minimumVersion).isEqualTo("0.0.0")
        assertThat(saved.maintenanceMessage).isNull()
    }

    @Test
    fun `findFirst`() {
        appConfigRepository.save(AppConfig())

        val found = appConfigRepository.findFirst()

        assertThat(found).isNotNull
        assertThat(found?.maintenanceMode).isFalse()
    }

    @Test
    fun `findFirst - not found`() {
        val found = appConfigRepository.findFirst()

        assertThat(found).isNull()
    }
}
