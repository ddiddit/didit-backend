package com.didit.domain.app

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Table(name = "app_configs")
@Entity
class AppConfig(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var maintenanceMode: Boolean = false,
    @Column(columnDefinition = "TEXT")
    var maintenanceMessage: String? = null,
    @Column(nullable = false, length = 20)
    var minimumVersion: String = "0.0.0",
) : BaseEntity() {
    fun update(request: AppConfigUpdateRequest) {
        maintenanceMode = request.maintenanceMode
        maintenanceMessage = request.maintenanceMessage
        minimumVersion = request.minimumVersion
    }
}
