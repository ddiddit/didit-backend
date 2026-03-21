package com.didit.docs

import com.didit.adapter.webapi.exception.ApiControllerAdvice
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@ExtendWith(RestDocumentationExtension::class)
abstract class RestDocsSupport {
    protected lateinit var mockMvc: MockMvc
    protected val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @BeforeEach
    fun setUp(provider: RestDocumentationContextProvider) {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(initController())
                .setControllerAdvice(ApiControllerAdvice())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(LocalValidatorFactoryBean().also { it.afterPropertiesSet() })
                .apply<StandaloneMockMvcBuilder>(documentationConfiguration(provider))
                .build()
    }

    abstract fun initController(): Any
}
