package com.didit.adapter.auth.jwt

import com.didit.adapter.auth.security.CustomUserDetails
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class JwtFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    private val logger = KotlinLogging.logger {}

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            authenticationProcess(request)
        } catch (ex: Exception) {
            logger.warn("JWT 인증 처리 중 예외 발생:${ex.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticationProcess(request: HttpServletRequest) {
        val token =
            resolveToken(request)
                ?: run {
                    logger.debug { "JWT 토큰이 존재하지 않습니다." }
                    return
                }
        if (!jwtProvider.validateToken(token)) {
            logger.warn("JWT 토큰이 만료되었거나 유효하지 않습니다.")
            return
        }
        if (SecurityContextHolder.getContext().authentication != null) {
            return
        }
        val userId = jwtProvider.getUserId(token)
        val role = jwtProvider.getRole(token)
        val userDetails = CustomUserDetails(userId, role)

        val authentication =
            UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities,
            )

        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        return null
    }
}
