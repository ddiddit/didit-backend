package com.didit.adapter.webapi.project.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProjectCreateRequest(
    @field:NotBlank
    @field:Size(max = 15)
    val name: String,
)
