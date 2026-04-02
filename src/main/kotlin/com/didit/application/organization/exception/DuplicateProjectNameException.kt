package com.didit.application.organization.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class DuplicateProjectNameException(
    userId: UUID,
    name: String,
) : BusinessException(
        OrganizationErrorCode.DUPLICATED_PROJECT_NAME,
        "userId:$userId, projectName:$name",
    )
