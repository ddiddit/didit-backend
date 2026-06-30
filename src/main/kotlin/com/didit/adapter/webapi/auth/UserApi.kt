package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.auth.dto.NicknameCheckResponse
import com.didit.adapter.webapi.auth.dto.OnboardingRequest
import com.didit.adapter.webapi.auth.dto.OnboardingRequestV2
import com.didit.adapter.webapi.auth.dto.UpdateProfileRequest
import com.didit.adapter.webapi.auth.dto.UpdateProfileRequestV2
import com.didit.adapter.webapi.auth.dto.UserProfileResponse
import com.didit.adapter.webapi.auth.dto.UserProfileResponseV2
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.achievement.provided.BadgeFinder
import com.didit.application.achievement.provided.UserLevelFinder
import com.didit.application.audit.Audit
import com.didit.application.audit.AuditAction
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserApi(
    private val userFinder: UserFinder,
    private val userRegister: UserRegister,
    private val badgeFinder: BadgeFinder,
    private val userLevelFinder: UserLevelFinder,
) {
    @GetMapping("/api/v1/users/nickname/check")
    fun checkNickname(
        @RequestParam nickname: String,
    ): SuccessResponse<NicknameCheckResponse> {
        val isDuplicate = userFinder.existsByNickname(nickname)
        return SuccessResponse.of(NicknameCheckResponse(isDuplicate = isDuplicate))
    }

    @Deprecated("Use v2 endpoint", replaceWith = ReplaceWith("onboardingV2"))
    @Audit(AuditAction.USER_SIGNED_UP)
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/api/v1/users/onboarding")
    fun onboarding(
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: OnboardingRequest,
    ) {
        userRegister.register(
            userId = userId,
            nickname = request.nickname,
            job = request.job,
            marketingAgreed = request.marketingAgreed,
            nightPushAgreed = request.nightPushAgreed,
        )
    }

    @Audit(AuditAction.USER_SIGNED_UP)
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/api/v2/users/onboarding")
    fun onboardingV2(
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: OnboardingRequestV2,
    ) {
        userRegister.registerV2(
            userId = userId,
            nickname = request.nickname,
            job = request.job,
            age = request.age,
            experience = request.experience,
            marketingAgreed = request.marketingAgreed,
            nightPushAgreed = request.nightPushAgreed,
        )
    }

    @Deprecated("Use v2 endpoint", replaceWith = ReplaceWith("getProfileV2"))
    @RequireAuth
    @GetMapping("/api/v1/users/profile")
    fun getProfile(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<UserProfileResponse> {
        val user = userFinder.findByIdOrThrow(userId)
        val recentBadges = badgeFinder.findRecent(userId)

        return SuccessResponse.of(UserProfileResponse.from(user, recentBadges))
    }

    @RequireAuth
    @GetMapping("/api/v2/users/profile")
    fun getProfileV2(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<UserProfileResponseV2> {
        val user = userFinder.findByIdOrThrow(userId)
        val recentBadges = badgeFinder.findRecent(userId)
        val currentLevel = userLevelFinder.getCurrentLevel(userId)

        return SuccessResponse.of(UserProfileResponseV2.from(user, currentLevel, recentBadges))
    }

    @Deprecated("Use v2 endpoint", replaceWith = ReplaceWith("updateProfileV2"))
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/api/v1/users/profile")
    fun updateProfile(
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: UpdateProfileRequest,
    ) {
        userRegister.updateProfile(
            userId = userId,
            nickname = request.nickname,
            job = request.job,
        )
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/api/v2/users/profile")
    fun updateProfileV2(
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: UpdateProfileRequestV2,
    ) {
        userRegister.updateProfileV2(
            userId = userId,
            nickname = request.nickname,
            job = request.job,
            age = request.age,
            experience = request.experience,
        )
    }
}
