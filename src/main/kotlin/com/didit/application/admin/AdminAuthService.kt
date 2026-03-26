package com.didit.application.admin

import com.didit.application.admin.dto.AdminRefreshResponse
import com.didit.application.admin.dto.AdminTokenResponse
import com.didit.application.admin.exception.AdminInvalidPasswordException
import com.didit.application.admin.exception.AdminNotActiveException
import com.didit.application.admin.exception.AdminNotFoundException
import com.didit.application.admin.exception.ExpiredAdminRefreshTokenException
import com.didit.application.admin.exception.InvalidAdminRefreshTokenException
import com.didit.application.admin.provided.AdminAuth
import com.didit.application.admin.provided.AdminFinder
import com.didit.application.admin.required.AdminRefreshTokenRepository
import com.didit.application.admin.required.AdminRepository
import com.didit.application.admin.required.AdminTokenProvider
import com.didit.application.admin.required.PasswordEncryptor
import com.didit.domain.admin.AdminRefreshToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminAuthService(
    private val adminRepository: AdminRepository,
    private val adminRefreshTokenRepository: AdminRefreshTokenRepository,
    private val adminFinder: AdminFinder,
    private val adminTokenProvider: AdminTokenProvider,
    private val passwordEncryptor: PasswordEncryptor,
) : AdminAuth {
    @Transactional
    override fun login(
        email: String,
        password: String,
    ): AdminTokenResponse {
        val admin = adminRepository.findByEmail(email) ?: throw AdminNotFoundException()
        check(admin.isActive) { throw AdminNotActiveException() }
        check(passwordEncryptor.matches(password, admin.password)) { throw AdminInvalidPasswordException() }

        adminRefreshTokenRepository.deleteByAdminId(admin.id)
        val newRefreshToken = adminTokenProvider.generateRefreshToken()
        adminRefreshTokenRepository.save(
            AdminRefreshToken.create(admin.id, newRefreshToken, adminTokenProvider.getRefreshTokenExpiresAt()),
        )

        return AdminTokenResponse(
            accessToken = adminTokenProvider.generateAccessToken(admin.id),
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    override fun logout(adminId: UUID) {
        adminRefreshTokenRepository.deleteByAdminId(adminId)
    }

    @Transactional
    override fun refresh(refreshToken: String): AdminRefreshResponse {
        val storedToken =
            adminRefreshTokenRepository.findByToken(refreshToken)
                ?: throw InvalidAdminRefreshTokenException()

        if (storedToken.isExpired()) throw ExpiredAdminRefreshTokenException()

        val admin = adminFinder.findByIdOrThrow(storedToken.adminId)
        val newRefreshToken = adminTokenProvider.generateRefreshToken()
        storedToken.rotate(newRefreshToken, adminTokenProvider.getRefreshTokenExpiresAt())
        adminRefreshTokenRepository.save(storedToken)

        return AdminRefreshResponse(
            accessToken = adminTokenProvider.generateAccessToken(admin.id),
            refreshToken = newRefreshToken,
        )
    }
}
