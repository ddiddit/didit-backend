package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.organization.dto.ProjectCreateRequest
import com.didit.adapter.webapi.organization.dto.ProjectListResponse
import com.didit.adapter.webapi.organization.dto.ProjectOrderRequest
import com.didit.adapter.webapi.organization.dto.RetrospectiveListResponse
import com.didit.adapter.webapi.organization.dto.UpdateProjectNameRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.organization.provided.ProjectFinder
import com.didit.application.organization.provided.ProjectModifier
import com.didit.application.organization.provided.ProjectRegister
import com.didit.application.retrospect.provided.RetrospectiveFinder
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val projectFinder: ProjectFinder,
    private val projectModifier: ProjectModifier,
    private val retrospectiveFinder: RetrospectiveFinder,
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

    @RequireAuth
    @GetMapping
    fun list(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<ProjectListResponse>> {
        val projects = projectFinder.findAllByUserId(userId).map { ProjectListResponse.from(it) }
        return SuccessResponse.of(projects)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{projectId}/name")
    fun update(
        @CurrentUserId userId: UUID,
        @PathVariable("projectId") projectId: UUID,
        @RequestBody request: UpdateProjectNameRequest,
    ) {
        projectModifier.updateName(userId, projectId, request.name)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{projectId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable projectId: UUID,
    ) {
        projectModifier.deleteProject(userId, projectId)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/order")
    fun reorder(
        @CurrentUserId userId: UUID,
        @RequestBody request: ProjectOrderRequest,
    ) {
        projectRegister.reorder(userId, request.projectIds)
    }

    @RequireAuth
    @GetMapping("/{projectId}")
    fun findByProject(
        @CurrentUserId userId: UUID,
        @PathVariable("projectId") projectId: UUID,
    ): SuccessResponse<List<RetrospectiveListResponse>> {
        val retrospectives =
            retrospectiveFinder.findByProject(userId, projectId).map { RetrospectiveListResponse.from(it) }

        return SuccessResponse.of(retrospectives)
    }
}
