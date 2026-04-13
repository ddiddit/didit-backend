package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectWithProjectAndTagResponse
import com.didit.application.retrospect.provided.RetrospectiveFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v2/retrospectives")
@RestController
class RetrospectV2Api(
    private val retrospectiveFinder: RetrospectiveFinder,
) {
    @RequireAuth
    @GetMapping("/{retrospectiveId}")
    fun findById(
        @CurrentUserId userId: UUID,
        @PathVariable("retrospectiveId") retrospectiveId: UUID,
    ): SuccessResponse<RetrospectWithProjectAndTagResponse> {
        val result = retrospectiveFinder.findRetrospectWithProjectAndTags(retrospectiveId, userId)

        return SuccessResponse.of(
            RetrospectWithProjectAndTagResponse.from(result),
        )
    }
}
