package com.didit.adapter.webapi.users

import com.didit.adapter.auth.security.CustomUserDetails
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.users.provided.WithdrawUseCase
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UsersController(
    private val withdrawUseCase: WithdrawUseCase,
) {
    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
    ): SuccessResponse<Void?> {
        withdrawUseCase.execute(userDetails.getUserId())
        return SuccessResponse.of(null)
    }
}
