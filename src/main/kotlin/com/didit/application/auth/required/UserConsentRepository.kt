package com.didit.application.auth.required

import com.didit.domain.auth.UserConsent
import org.springframework.data.repository.Repository
import java.util.UUID

interface UserConsentRepository : Repository<UserConsent, UUID> {
    fun save(consent: UserConsent): UserConsent
}
