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

@RequestMapping("/api/v2/tags")
@RestController
class TagV2Api(
    private val retrospectiveFinder: RetrospectiveFinder,
) {
    @RequireAuth
    @GetMapping("/{tagId}")
    fun findByTagId(
        @CurrentUserId userId: UUID,
        @PathVariable("tagId") tagId: UUID,
    ): SuccessResponse<List<RetrospectiveListItemV2Response>> {
        val results = retrospectiveFinder.findByTagIdWithProjectAndTags(userId, tagId)

        return SuccessResponse.of(results.map { RetrospectiveListItemV2Response.from(it) })
    }
}
