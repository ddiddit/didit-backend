package com.didit.adapter.auth.social.oidc

interface OidcVerifier<T> {
    fun verify(idToken: String): T
}
