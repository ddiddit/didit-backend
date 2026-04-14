package com.didit.adapter.config

import com.didit.application.retrospect.required.RetrospectivePolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RetrospectiveProperties(
    @Value("\${retrospective.whitelist-emails:}")
    private val whitelistEmailsList: String,
) : RetrospectivePolicy {
    private val whitelistEmails =
        whitelistEmailsList
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    override fun isWhitelisted(email: String?): Boolean = email?.lowercase() in whitelistEmails
}
