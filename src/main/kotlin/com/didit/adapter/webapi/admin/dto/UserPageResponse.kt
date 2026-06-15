package com.didit.adapter.webapi.admin.dto

import com.didit.application.admin.provided.UserListResult

data class UserPageResponse(
    val content: List<UserListResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(result: UserListResult) =
            UserPageResponse(
                content = result.content.map { UserListResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
    }
}
