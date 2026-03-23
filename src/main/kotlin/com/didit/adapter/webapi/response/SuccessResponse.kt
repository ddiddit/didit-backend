package com.didit.adapter.webapi.response

data class SuccessResponse<T>(
    val data: T,
) {
    companion object {
        fun <T> of(data: T): SuccessResponse<T> = SuccessResponse(data = data)
    }
}
