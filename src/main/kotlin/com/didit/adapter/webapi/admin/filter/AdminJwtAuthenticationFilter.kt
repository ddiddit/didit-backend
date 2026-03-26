package com.didit.adapter.webapi.admin.filter

import com.didit.adapter.security.AdminJwtTokenParser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AdminJwtAuthenticationFilter(
    private val adminJwtTokenParser: AdminJwtTokenParser,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !request.requestURI.startsWith("/api/v1/admin")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        authenticate(request)
        filterChain.doFilter(request, response)
    }

    private fun authenticate(request: HttpServletRequest) {
        val token = extractToken(request) ?: return
        val (adminId, role) = adminJwtTokenParser.getAdminIdAndRole(token) ?: return
        setAuthentication(adminId.toString(), role)
    }

    private fun setAuthentication(
        adminId: String,
        role: String,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                adminId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_$role")),
            )
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization") ?: return null
        return bearerToken.takeIf { it.startsWith("Bearer ") }?.substring(7)
    }
}
