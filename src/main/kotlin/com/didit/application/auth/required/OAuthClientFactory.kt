package com.didit.application.auth.required

import com.didit.application.auth.exception.UnsupportedOAuthProviderException
import com.didit.domain.auth.Provider

class OAuthClientFactory(
    private val clients: Map<Provider, OAuthClient>,
) {
    fun getClient(provider: Provider): OAuthClient = clients[provider] ?: throw UnsupportedOAuthProviderException()
}
