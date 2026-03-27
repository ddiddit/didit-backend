package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.retrospect.dto.CompleteRetrospectiveResponse
import com.didit.application.retrospect.dto.GetRetrospectiveResponse
import com.didit.application.retrospect.dto.StartRetrospectiveResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.provided.RetrospectService
import com.didit.application.retrospect.RetrospectQueryService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/retrospectives")
class RetrospectController(
    private val retrospectService: RetrospectService,
    private val retrospectQueryService: RetrospectQueryService,
) {
    @PostMapping("/start")
    fun startRetrospective(
        @AuthenticationPrincipal userDetails: UserDetails,
    ): SuccessResponse<StartRetrospectiveResponse> {
        val userId = UUID.fromString(userDetails.username)
        val response = retrospectService.startRetrospective(userId)
        return SuccessResponse.of(response)
    }

    @PostMapping("/answer")
    fun submitAnswer(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @RequestBody request: SubmitAnswerRequest,
    ): SuccessResponse<SubmitAnswerResponse> {
        val userId = UUID.fromString(userDetails.username)
        val response = retrospectService.submitAnswer(userId, request.answer)
        return SuccessResponse.of(response)
    }

    @GetMapping
    fun getRetrospective(
        @AuthenticationPrincipal userDetails: UserDetails,
    ): SuccessResponse<GetRetrospectiveResponse> {
        val userId = UUID.fromString(userDetails.username)
        val response = retrospectQueryService.getRetrospective(userId)
        return SuccessResponse.of(response)
    }

    @GetMapping("/complete")
    fun completeRetrospective(
        @AuthenticationPrincipal userDetails: UserDetails,
    ): SuccessResponse<CompleteRetrospectiveResponse> {
        val userId = UUID.fromString(userDetails.username)
        val response = retrospectQueryService.completeRetrospective(userId)
        return SuccessResponse.of(response)
    }
}

data class SubmitAnswerRequest(
    val answer: String,
)
