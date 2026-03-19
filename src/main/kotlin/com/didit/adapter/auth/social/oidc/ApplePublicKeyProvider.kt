package com.didit.adapter.auth.social.oidc

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

@Component
class ApplePublicKeyProvider {
    private val objectMapper = ObjectMapper()
    private val cache: MutableMap<String, RSAPublicKey> = ConcurrentHashMap()

    fun getPublicKey(kid: String): RSAPublicKey {
        cache[kid]?.let { return it }

        val keys = fetchAppleKeys()
        val key =
            keys.firstOrNull { it["kid"] == kid }
                ?: throw IllegalArgumentException("Apple public key not found")

        val publicKey = generatePublicKey(key["n"] as String, key["e"] as String)
        cache[kid] = publicKey
        return publicKey
    }

    private fun fetchAppleKeys(): List<Map<String, Any>> {
        val url = URL("https://appleid.apple.com/auth/keys")
        val connection =
            url.openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val map: Map<String, Any> = objectMapper.readValue(json, Map::class.java) as Map<String, Any>
        return map["keys"] as List<Map<String, Any>>
    }

    private fun generatePublicKey(
        n: String,
        e: String,
    ): RSAPublicKey {
        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(n))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(e))
        val spec = RSAPublicKeySpec(modulus, exponent)
        val factory = java.security.KeyFactory.getInstance("RSA")
        return factory.generatePublic(spec) as RSAPublicKey
    }
}
