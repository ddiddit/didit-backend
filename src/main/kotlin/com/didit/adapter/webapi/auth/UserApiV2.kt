package com.didit.adapter.webapi.auth

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.auth.dto.OnboardingRequestV2
import com.didit.adapter.webapi.auth.dto.UpdateProfileRequestV2
import com.didit.adapter.webapi.auth.dto.UserProfileResponseV2
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.achievement.provided.BadgeFinder
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v2/users")
@RestController
class UserApiV2(
    private val userFinder: UserFinder,
    private val userRegister: UserRegister,
    private val badgeFinder: BadgeFinder,
) {
    @Audit(AuditAction.USER_SIGNED_UP)
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/onboarding")
    fun onboarding(
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

    @RequireAuth
    @GetMapping("/profile")
    fun getProfile(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<UserProfileResponseV2> {
        val user = userFinder.findByIdOrThrow(userId)
        val recentBadges = badgeFinder.findRecent(userId)

        return SuccessResponse.of(UserProfileResponseV2.from(user, recentBadges))
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/profile")
    fun updateProfile(
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
