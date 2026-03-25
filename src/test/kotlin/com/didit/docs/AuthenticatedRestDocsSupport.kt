package com.didit.docs

import com.didit.adapter.webapi.auth.resolver.CurrentUserIdResolver
import com.didit.adapter.webapi.exception.ApiControllerAdvice
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.util.UUID

abstract class AuthenticatedRestDocsSupport : RestDocsSupport() {
    protected val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUpSecurityContext(provider: RestDocumentationContextProvider) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(initController())
                .setControllerAdvice(ApiControllerAdvice())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(LocalValidatorFactoryBean().also { it.afterPropertiesSet() })
                .setCustomArgumentResolvers(CurrentUserIdResolver())
                .apply<StandaloneMockMvcBuilder>(documentationConfiguration(provider))
                .build()
    }
}
