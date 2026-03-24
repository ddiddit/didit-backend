package com.didit.adapter.auth.jwt

import com.didit.adapter.auth.security.CustomUserDetails
import com.didit.adapter.auth.security.CustomUserDetailsService
import com.didit.application.users.exception.UserNotFoundException
import com.didit.application.users.exception.UserWithdrawException
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
    private val userDetailsService: CustomUserDetailsService,
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
        val userDetails: CustomUserDetails =
            try {
                userDetailsService.loadUserById(userId)
            } catch (ex: UserNotFoundException) {
                logger.warn("사용자 없음: ${ex.message}")
                return
            } catch (ex: UserWithdrawException) {
                logger.warn("탈퇴 회원 접근 시도: ${ex.message}")
                return
            }

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
