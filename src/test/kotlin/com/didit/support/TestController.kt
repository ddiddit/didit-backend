package com.didit.support

import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/test")
@RestController
class TestController {
    @GetMapping("/success-data")
    fun successData(): SuccessResponse<Map<String, String>> = SuccessResponse.of(mapOf("id" to "1", "name" to "테스트"))

    @GetMapping("/success-message")
    fun successMessage(): SuccessResponse<Unit> = SuccessResponse.of("처리되었습니다.")

    @GetMapping("/business-error")
    fun businessError(): String = throw BusinessException(ErrorCode.INVALID_REQUEST)

    @GetMapping("/server-error")
    fun serverError(): String = throw RuntimeException("서버 에러")

    @PostMapping("/validation-error")
    fun validationError(
        @Valid @RequestBody request: TestRequest,
    ): String = "ok"
}

data class TestRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String?,
)
