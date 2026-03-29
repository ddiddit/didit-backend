package com.didit.adapter.webapi.response

data class PageResponse<T>(
    val data: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <T> of(
            data: List<T>,
            page: Int,
            size: Int,
            totalElements: Long,
        ): PageResponse<T> =
            PageResponse(
                data = data,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = ((totalElements + size - 1) / size).toInt(),
                hasNext = (page + 1) * size < totalElements,
            )
    }
}
