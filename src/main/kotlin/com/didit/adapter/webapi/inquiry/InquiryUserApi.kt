package com.didit.adapter.webapi.inquiry

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.inquiry.dto.InquiryRequest
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.inquiry.provided.InquiryInfoFinder
import com.didit.application.inquiry.provided.InquiryRegister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inquiry")
class InquiryUserApi(
    private val inquiryInfoFinder: InquiryInfoFinder,
    private val inquiryRegister: InquiryRegister,
) {
    @RequireAuth
    @GetMapping
    fun info(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<String?> = SuccessResponse.of(inquiryInfoFinder.findEmail(userId))

    @RequireAuth
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(
        @CurrentUserId userId: UUID,
        @RequestBody request: InquiryRequest,
    ) {
        inquiryRegister.register(userId, request)
    }
}
