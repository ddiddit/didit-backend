package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.retrospect.RetrospectQueryService
import com.didit.application.retrospect.dto.CompleteRetrospectiveResponse
import com.didit.application.retrospect.dto.GetRetrospectiveResponse
import com.didit.application.retrospect.dto.StartRetrospectiveResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.provided.RetrospectService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/retrospectives")
@RestController
class RetrospectApi(
    private val retrospectService: RetrospectService,
    private val retrospectQueryService: RetrospectQueryService,
) {
    @RequireAuth
    @PostMapping("/start")
    fun startRetrospective(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<StartRetrospectiveResponse> = SuccessResponse.of(retrospectService.startRetrospective(userId))

    @RequireAuth
    @PostMapping("/answer")
    fun submitAnswer(
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: SubmitAnswerRequest,
    ): SuccessResponse<SubmitAnswerResponse> = SuccessResponse.of(retrospectService.submitAnswer(userId, request.answer))

    @RequireAuth
    @GetMapping
    fun getRetrospective(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<GetRetrospectiveResponse> = SuccessResponse.of(retrospectQueryService.getRetrospective(userId))

    @RequireAuth
    @GetMapping("/complete")
    fun completeRetrospective(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<CompleteRetrospectiveResponse> = SuccessResponse.of(retrospectQueryService.completeRetrospective(userId))
}

data class SubmitAnswerRequest(
    val answer: String,
)
