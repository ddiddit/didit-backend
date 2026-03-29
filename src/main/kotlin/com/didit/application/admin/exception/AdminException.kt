package com.didit.application.admin.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class AdminNotFoundException(
    adminId: UUID? = null,
) : BusinessException(
        AdminErrorCode.ADMIN_NOT_FOUND,
        adminId?.let { "adminId: $it" } ?: AdminErrorCode.ADMIN_NOT_FOUND.detail,
    )

class AdminNotActiveException : BusinessException(AdminErrorCode.ADMIN_NOT_ACTIVE)

class AdminInvalidPasswordException : BusinessException(AdminErrorCode.ADMIN_INVALID_PASSWORD)

class InvalidAdminRefreshTokenException : BusinessException(AdminErrorCode.INVALID_ADMIN_REFRESH_TOKEN)

class ExpiredAdminRefreshTokenException : BusinessException(AdminErrorCode.EXPIRED_ADMIN_REFRESH_TOKEN)

class InvalidAdminInviteTokenException : BusinessException(AdminErrorCode.INVALID_ADMIN_INVITE_TOKEN)

class DuplicateAdminInviteException : BusinessException(AdminErrorCode.DUPLICATE_ADMIN_INVITE)

class DuplicateAdminEmailException : BusinessException(AdminErrorCode.DUPLICATE_ADMIN_EMAIL)
