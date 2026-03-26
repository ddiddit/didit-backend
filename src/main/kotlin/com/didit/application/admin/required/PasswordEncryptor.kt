package com.didit.application.admin.required

interface PasswordEncryptor {
    fun encode(password: String): String

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean
}
