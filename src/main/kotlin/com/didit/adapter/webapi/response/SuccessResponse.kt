package com.didit.adapter.webapi.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuccessResponse<T>(
    val data: T? = null,
    val message: String? = null
) {
    companion object {
        fun <T> of(data: T, message: String): SuccessResponse<T> {
            return SuccessResponse(
                data = data,
                message = message
            )
        }

        fun <T> of(data: T): SuccessResponse<T> {
            return SuccessResponse(
                data = data,
                message = null
            )
        }

        fun of(message: String): SuccessResponse<Unit> {
            return SuccessResponse(
                data = null,
                message = message
            )
        }
    }
}
