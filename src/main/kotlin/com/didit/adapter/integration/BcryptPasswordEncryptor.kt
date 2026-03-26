package com.didit.adapter.integration

import com.didit.application.admin.required.PasswordEncryptor
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordEncryptor(
    private val passwordEncoder: PasswordEncoder,
) : PasswordEncryptor {
    override fun encode(password: String): String = passwordEncoder.encode(password)

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = passwordEncoder.matches(rawPassword, encodedPassword)
}
