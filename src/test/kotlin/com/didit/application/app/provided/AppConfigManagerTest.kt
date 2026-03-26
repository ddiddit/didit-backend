package com.didit.application.app.provided

import com.didit.domain.app.AppConfig
import com.didit.domain.app.AppConfigUpdateRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AppConfigManagerTest {
    @Mock
    lateinit var appConfigManager: AppConfigManager

    @Test
    fun `getAppConfig`() {
        val config = AppConfig()
        whenever(appConfigManager.getAppConfig()).thenReturn(config)

        val result = appConfigManager.getAppConfig()

        verify(appConfigManager).getAppConfig()
        assertThat(result.maintenanceMode).isFalse()
        assertThat(result.minimumVersion).isEqualTo("0.0.0")
    }

    @Test
    fun `updateAppConfig`() {
        val request =
            AppConfigUpdateRequest(
                maintenanceMode = true,
                maintenanceMessage = "점검 중입니다.",
                minimumVersion = "1.0.0",
            )
        val updated = AppConfig().apply { update(request) }
        whenever(appConfigManager.updateAppConfig(request)).thenReturn(updated)

        val result = appConfigManager.updateAppConfig(request)

        verify(appConfigManager).updateAppConfig(request)
        assertThat(result.maintenanceMode).isTrue()
        assertThat(result.maintenanceMessage).isEqualTo("점검 중입니다.")
        assertThat(result.minimumVersion).isEqualTo("1.0.0")
    }
}
