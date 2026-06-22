package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemV2Response
import com.didit.application.retrospect.provided.RetrospectiveFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v2/projects")
@RestController
class ProjectV2Api(
    private val retrospectiveFinder: RetrospectiveFinder,
) {
    @RequireAuth
    @GetMapping("/{projectId}")
    fun findByProject(
        @CurrentUserId userId: UUID,
        @PathVariable("projectId") projectId: UUID,
    ): SuccessResponse<List<RetrospectiveListItemV2Response>> {
        val results = retrospectiveFinder.findByProjectWithProjectAndTags(userId, projectId)

        return SuccessResponse.of(results.map { RetrospectiveListItemV2Response.from(it) })
    }
}
