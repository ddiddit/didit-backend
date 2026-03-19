package com.didit.adapter.auth.social.oidc.dto

data class AppleIdTokenPayload(
    val subject: String,
    val email: String?,
)
