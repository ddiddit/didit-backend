package com.didit.application.auth.required.social

interface KakaoAuthPort {
    fun getIdToken(code: String, redirectUri: String): String
}
