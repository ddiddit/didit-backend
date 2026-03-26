package com.didit.docs

import com.didit.adapter.webapi.admin.resolver.CurrentAdminIdResolver
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

abstract class AdminAuthenticatedRestDocsSupport : RestDocsSupport() {
    protected val adminId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUpSecurityContext(provider: RestDocumentationContextProvider) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                adminId.toString(),
                null,
                listOf(SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
            )
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(initController())
                .setControllerAdvice(ApiControllerAdvice())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(LocalValidatorFactoryBean().also { it.afterPropertiesSet() })
                .setCustomArgumentResolvers(CurrentAdminIdResolver())
                .apply<StandaloneMockMvcBuilder>(documentationConfiguration(provider))
                .build()
    }
}
