package com.didit.adapter.auth.social.oidc

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GoogleOidcVerifier(
    @Value("\${oauth.google.client-id}")
    private val googleClientId: String,
) : OidcVerifier<GoogleIdToken.Payload> {
    private val verifier: GoogleIdTokenVerifier =
        GoogleIdTokenVerifier
            .Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
            ).setAudience(listOf(googleClientId))
            .build()

    override fun verify(idToken: String): GoogleIdToken.Payload {
        val googleIdToken = verifier.verify(idToken) ?: throw IllegalArgumentException("Invalid Google ID token")
        return googleIdToken.payload
    }
}
