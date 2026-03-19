package com.didit.adapter.auth.social.oidc

import com.auth0.jwk.JwkProviderBuilder
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

@Component
class KakaoPublicKeyProvider {
    private val jwkProvider =
        JwkProviderBuilder("https://kauth.kakao.com/.well-known/jwks.json")
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    fun getPublicKey(kid: String): RSAPublicKey {
        val jwk = jwkProvider.get(kid)
        return jwk.publicKey as RSAPublicKey
    }
}
