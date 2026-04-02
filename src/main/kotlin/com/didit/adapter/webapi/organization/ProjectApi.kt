package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.organization.dto.ProjectCreateRequest
import com.didit.application.organization.provided.ProjectRegister
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/projects")
@RestController
class ProjectApi(
    private val projectRegister: ProjectRegister,
) {
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    fun create(
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: ProjectCreateRequest,
    ) {
        projectRegister.create(userId, request.name)
    }
}
