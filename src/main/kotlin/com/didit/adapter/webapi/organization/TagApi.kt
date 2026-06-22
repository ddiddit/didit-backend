package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.organization.dto.TagCreateRequest
import com.didit.adapter.webapi.organization.dto.TagCreateResponse
import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemV2Response
import com.didit.application.organization.provided.RetrospectTagFinder
import com.didit.application.organization.provided.TagFinder
import com.didit.application.organization.provided.TagModifier
import com.didit.application.organization.provided.TagRegister
import com.didit.application.retrospect.provided.RetrospectiveFinder
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class TagApi(
    private val tagRegister: TagRegister,
    private val tagFinder: TagFinder,
    private val tagModifier: TagModifier,
    private val retrospectTagFinder: RetrospectTagFinder,
    private val retrospectiveFinder: RetrospectiveFinder,
) {
    @RequireAuth
    @PostMapping("/api/v1/tags")
    fun create(
        @CurrentUserId userId: UUID,
        @RequestBody request: TagCreateRequest,
    ): SuccessResponse<TagCreateResponse> {
        val tag = tagRegister.create(userId, request.name)
        return SuccessResponse.of(TagCreateResponse.of(tag))
    }

    @RequireAuth
    @GetMapping("/api/v1/tags")
    fun findAllByUserId(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<TagListResponse>> {
        val tags = tagFinder.findAllByUserId(userId).map { TagListResponse.from(it) }
        return SuccessResponse.of(tags)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/tags/{tagId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable tagId: UUID,
    ) {
        tagModifier.delete(userId, tagId)
    }

    @Deprecated("Use v2 endpoint", replaceWith = ReplaceWith("findByTagIdV2"))
    @RequireAuth
    @GetMapping("/api/v1/tags/{tagId}")
    fun findByTagId(
        @CurrentUserId userId: UUID,
        @PathVariable tagId: UUID,
    ): SuccessResponse<List<RetrospectiveListItemResponse>> {
        val retrospects = retrospectTagFinder.findAllByTagId(tagId).map { RetrospectiveListItemResponse.from(it) }
        return SuccessResponse.of(retrospects)
    }

    @RequireAuth
    @GetMapping("/api/v2/tags/{tagId}")
    fun findByTagIdV2(
        @CurrentUserId userId: UUID,
        @PathVariable tagId: UUID,
    ): SuccessResponse<List<RetrospectiveListItemV2Response>> {
        val results = retrospectiveFinder.findByTagIdWithProjectAndTags(userId, tagId)

        return SuccessResponse.of(results.map { RetrospectiveListItemV2Response.from(it) })
    }
}
