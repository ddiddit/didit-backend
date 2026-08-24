package com.didit.application.auth.required

import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialIdentity
import org.springframework.data.repository.Repository
import java.util.UUID

interface SocialIdentityRepository : Repository<SocialIdentity, UUID> {
    fun save(identity: SocialIdentity): SocialIdentity

    fun delete(identity: SocialIdentity)

    fun deleteAllByUserId(userId: UUID)

    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): SocialIdentity?
}
