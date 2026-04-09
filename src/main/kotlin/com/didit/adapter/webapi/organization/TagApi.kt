package com.didit.adapter.webapi.organization

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.organization.dto.TagCreateRequest
import com.didit.adapter.webapi.organization.dto.TagCreateResponse
import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemResponse
import com.didit.application.organization.provided.RetrospectTagFinder
import com.didit.application.organization.provided.TagFinder
import com.didit.application.organization.provided.TagModifier
import com.didit.application.organization.provided.TagRegister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/tags")
@RestController
class TagApi(
    private val tagRegister: TagRegister,
    private val tagFinder: TagFinder,
    private val tagModifier: TagModifier,
    private val retrospectTagFinder: RetrospectTagFinder,
) {
    @RequireAuth
    @PostMapping
    fun create(
        @CurrentUserId userId: UUID,
        @RequestBody request: TagCreateRequest,
    ): SuccessResponse<TagCreateResponse> {
        val tag = tagRegister.create(userId, request.name)
        return SuccessResponse.of(TagCreateResponse.of(tag))
    }

    @RequireAuth
    @GetMapping
    fun findAllByUserId(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<TagListResponse>> {
        val tags = tagFinder.findAllByUserId(userId).map { TagListResponse.from(it) }
        return SuccessResponse.of(tags)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{tagId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable tagId: UUID,
    ) {
        tagModifier.delete(userId, tagId)
    }

    @RequireAuth
    @GetMapping("/{tagId}")
    fun findByTagId(
        @CurrentUserId userId: UUID,
        @PathVariable tagId: UUID,
    ): SuccessResponse<List<RetrospectiveListItemResponse>> {
        val retrospects = retrospectTagFinder.findAllByTagId(tagId).map { RetrospectiveListItemResponse.from(it) }
        return SuccessResponse.of(retrospects)
    }
}
