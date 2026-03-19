package com.didit.adapter.auth.social.oidc

import com.auth0.jwt.JWT
import com.didit.adapter.auth.social.oidc.dto.AppleIdTokenPayload
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppleOidcVerifier(
    @Value("\${oauth.apple.client-id}")
    private val appleClientId: String,
    private val keyProvider: ApplePublicKeyProvider,
) : OidcVerifier<AppleIdTokenPayload> {
    override fun verify(idToken: String): AppleIdTokenPayload {
        val decodedJWT = JWT.decode(idToken)
        val kid = decodedJWT.keyId ?: throw IllegalArgumentException("Missing kid in Apple token")

        val publicKey = keyProvider.getPublicKey(kid)
        val verifier =
            com.auth0.jwt.JWT
                .require(
                    com.auth0.jwt.algorithms.Algorithm
                        .RSA256(publicKey, null),
                ).withIssuer("https://appleid.apple.com")
                .withAudience(appleClientId)
                .build()

        val jwt = verifier.verify(idToken)
        return AppleIdTokenPayload(
            subject = jwt.subject ?: throw IllegalArgumentException("Missing sub"),
            email = jwt.getClaim("email")?.asString(),
        )
    }
}
