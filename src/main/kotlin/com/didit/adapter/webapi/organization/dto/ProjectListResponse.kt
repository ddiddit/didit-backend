package com.didit.adapter.webapi.organization.dto

import com.didit.domain.organization.Project
import java.util.UUID

data class ProjectListResponse(
    val id: UUID,
    val name: String,
) {
    companion object {
        fun from(project: Project): ProjectListResponse =
            ProjectListResponse(
                id = project.id,
                name = project.name,
            )
    }
}
