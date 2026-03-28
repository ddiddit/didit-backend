package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.retrospect.RetrospectQueryService
import com.didit.application.retrospect.dto.CompleteRetrospectiveResponse
import com.didit.application.retrospect.dto.GetRetrospectiveResponse
import com.didit.application.retrospect.dto.StartRetrospectiveResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.provided.RetrospectService
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
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
    fun startRetrospective(): SuccessResponse<StartRetrospectiveResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(authentication?.name ?: throw IllegalArgumentException("인증된 사용자가 아닙니다."))
        val response = retrospectService.startRetrospective(userId)
        return SuccessResponse.of(response)
    }

    @PostMapping("/answer")
    fun submitAnswer(
        @Valid @RequestBody request: SubmitAnswerRequest,
    ): SuccessResponse<SubmitAnswerResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(authentication?.name ?: throw IllegalArgumentException("인증된 사용자가 아닙니다."))
        val response = retrospectService.submitAnswer(userId, request.answer)
        return SuccessResponse.of(response)
    }

    @GetMapping
    fun getRetrospective(): SuccessResponse<GetRetrospectiveResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(authentication?.name ?: throw IllegalArgumentException("인증된 사용자가 아닙니다."))
        val response = retrospectQueryService.getRetrospective(userId)
        return SuccessResponse.of(response)
    }

    @GetMapping("/complete")
    fun completeRetrospective(): SuccessResponse<CompleteRetrospectiveResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(authentication?.name ?: throw IllegalArgumentException("인증된 사용자가 아닙니다."))
        val response = retrospectQueryService.completeRetrospective(userId)
        return SuccessResponse.of(response)
    }
}

data class SubmitAnswerRequest(
    val answer: String,
)
