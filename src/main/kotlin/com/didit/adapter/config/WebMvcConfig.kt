package com.didit.adapter.config

import com.didit.adapter.webapi.admin.resolver.CurrentAdminIdResolver
import com.didit.adapter.webapi.auth.resolver.CurrentUserIdResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val currentUserIdResolver: CurrentUserIdResolver,
    private val currentAdminIdResolver: CurrentAdminIdResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserIdResolver)
        resolvers.add(currentAdminIdResolver)
    }
}
