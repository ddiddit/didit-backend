package com.didit.application.retrospect.required

interface RetrospectivePolicy {
    fun isWhitelisted(email: String?): Boolean
}
