package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveMemoContentRequest
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveMemoResponse
import com.didit.application.retrospect.provided.RetrospectiveMemoFinder
import com.didit.application.retrospect.provided.RetrospectiveMemoModifier
import com.didit.application.retrospect.provided.RetrospectiveMemoRegister
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RetrospectiveMemoApi(
    private val memoRegister: RetrospectiveMemoRegister,
    private val memoFinder: RetrospectiveMemoFinder,
    private val memoModifier: RetrospectiveMemoModifier,
) {
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v2/retrospectives/{retrospectiveId}/memos")
    fun create(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @Valid @RequestBody request: RetrospectiveMemoContentRequest,
    ): SuccessResponse<RetrospectiveMemoResponse> =
        SuccessResponse.of(RetrospectiveMemoResponse.from(memoRegister.create(retrospectiveId, userId, request.content)))

    @RequireAuth
    @GetMapping("/api/v2/retrospectives/{retrospectiveId}/memos")
    fun findAll(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<List<RetrospectiveMemoResponse>> =
        SuccessResponse.of(memoFinder.findAll(retrospectiveId, userId).map(RetrospectiveMemoResponse::from))

    @RequireAuth
    @PatchMapping("/api/v2/retrospectives/{retrospectiveId}/memos/{memoId}")
    fun update(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @PathVariable memoId: UUID,
        @Valid @RequestBody request: RetrospectiveMemoContentRequest,
    ): SuccessResponse<RetrospectiveMemoResponse> =
        SuccessResponse.of(
            RetrospectiveMemoResponse.from(memoModifier.update(retrospectiveId, memoId, userId, request.content)),
        )

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v2/retrospectives/{retrospectiveId}/memos/{memoId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @PathVariable memoId: UUID,
    ) {
        memoModifier.delete(retrospectiveId, memoId, userId)
    }
}
