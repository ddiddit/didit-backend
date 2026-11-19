package com.didit.adapter.integration.oauth

import com.didit.application.auth.required.AppleTokenProtector
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AesAppleTokenProtector(
    @Value("\${oauth.apple.token-encryption-key:\${oauth.apple.private-key-pem-base64:}}") keyMaterial: String,
) : AppleTokenProtector {
    private val key =
        keyMaterial.takeIf(String::isNotBlank)?.let {
            SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(it.toByteArray()), "AES")
        }
    private val secureRandom = SecureRandom()

    override fun protect(refreshToken: String): String {
        val iv = ByteArray(IV_SIZE).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, configuredKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encrypted = cipher.doFinal(refreshToken.toByteArray(StandardCharsets.UTF_8))
        return "v1:${Base64.getEncoder().encodeToString(iv + encrypted)}"
    }

    override fun unprotect(protectedToken: String): String {
        require(protectedToken.startsWith("v1:")) { "Unsupported Apple token format" }
        val decoded = Base64.getDecoder().decode(protectedToken.removePrefix("v1:"))
        require(decoded.size > IV_SIZE) { "Invalid Apple token payload" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, configuredKey(), GCMParameterSpec(TAG_SIZE_BITS, decoded.copyOfRange(0, IV_SIZE)))
        return String(cipher.doFinal(decoded.copyOfRange(IV_SIZE, decoded.size)), StandardCharsets.UTF_8)
    }

    private fun configuredKey() = requireNotNull(key) { "Apple token encryption key must be configured" }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}
