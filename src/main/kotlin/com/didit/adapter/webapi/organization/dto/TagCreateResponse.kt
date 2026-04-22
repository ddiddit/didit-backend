package com.didit.adapter.webapi.organization.dto

import com.didit.domain.organization.Tag
import java.util.UUID

data class TagCreateResponse(
    val id: UUID,
    val name: String,
) {
    companion object {
        fun of(tag: Tag): TagCreateResponse =
            TagCreateResponse(
                id = tag.id,
                name = tag.name,
            )
    }
}
