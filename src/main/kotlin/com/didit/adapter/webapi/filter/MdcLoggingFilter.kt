package com.didit.adapter.webapi.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class MdcLoggingFilter : OncePerRequestFilter() {
    companion object {
        private const val REQUEST_ID = "requestId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = UUID.randomUUID().toString().substring(0, 8)

        MDC.put(REQUEST_ID, requestId)
        response.setHeader("X-Request-Id", requestId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID)
        }
    }
}
