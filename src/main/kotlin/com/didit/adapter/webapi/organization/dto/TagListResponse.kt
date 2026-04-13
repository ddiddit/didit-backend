package com.didit.adapter.webapi.organization.dto

import com.didit.domain.organization.Tag
import java.util.UUID

data class TagListResponse(
    val id: UUID,
    val name: String,
) {
    companion object {
        fun from(tag: Tag): TagListResponse =
            TagListResponse(
                id = tag.id,
                name = tag.name,
            )
    }
}
