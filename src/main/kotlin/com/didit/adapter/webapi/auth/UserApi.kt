package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.auth.dto.NicknameCheckResponse
import com.didit.adapter.webapi.auth.dto.OnboardingRequest
import com.didit.adapter.webapi.auth.dto.UpdateProfileRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/users")
@RestController
class UserApi(
    private val userFinder: UserFinder,
    private val userRegister: UserRegister,
) {
    @GetMapping("/nickname/check")
    fun checkNickname(
        @RequestParam nickname: String,
    ): SuccessResponse<NicknameCheckResponse> {
        val isDuplicate = userFinder.existsByNickname(nickname)
        return SuccessResponse.of(NicknameCheckResponse(isDuplicate = isDuplicate))
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/onboarding")
    @RequireAuth
    fun onboarding(
        @CurrentUserId userId: UUID,
        @RequestBody request: OnboardingRequest,
    ) {
        userRegister.register(
            userId = userId,
            nickname = request.nickname,
            job = request.job,
            marketingAgreed = request.marketingAgreed,
            nightPushAgreed = request.nightPushAgreed,
        )
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/profile")
    @RequireAuth
    fun updateProfile(
        @CurrentUserId userId: UUID,
        @RequestBody request: UpdateProfileRequest,
    ) {
        userRegister.updateProfile(
            userId = userId,
            nickname = request.nickname,
            job = request.job,
        )
    }
}
