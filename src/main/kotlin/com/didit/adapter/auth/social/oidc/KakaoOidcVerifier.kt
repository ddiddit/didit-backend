package com.didit.adapter.auth.social.oidc

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.didit.adapter.auth.social.oidc.dto.KakaoIdTokenPayload
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class KakaoOidcVerifier(
    @Value("\${oauth.kakao.client-id}")
    private val kakaoClientId: String,
    private val keyProvider: KakaoPublicKeyProvider,
) : OidcVerifier<KakaoIdTokenPayload> {
    override fun verify(idToken: String): KakaoIdTokenPayload {
        try {
            val decodedJWT = JWT.decode(idToken)

            val kid =
                decodedJWT.keyId
                    ?: throw IllegalArgumentException("Missing kid in Kakao token")

            val publicKey = keyProvider.getPublicKey(kid)

            val verifier =
                JWT
                    .require(
                        Algorithm.RSA256(publicKey, null),
                    ).withIssuer("https://kauth.kakao.com")
                    .withAudience(kakaoClientId)
                    .build()

            val jwt = verifier.verify(idToken)

            return KakaoIdTokenPayload(
                subject = jwt.subject ?: throw IllegalArgumentException("Missing sub"),
                email = jwt.getClaim("email")?.asString(),
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Kakao ID Token", e)
        }
    }
}
