package com.didit.adapter.auth.social.oidc.dto

data class KakaoIdTokenPayload(
    val subject: String,
    val email: String?,
)
