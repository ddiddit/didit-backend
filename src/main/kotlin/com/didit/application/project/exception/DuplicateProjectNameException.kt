package com.didit.application.project.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class DuplicateProjectNameException(
    userId: UUID,
    name: String,
) : BusinessException(
        ProjectErrorCode.DUPLICATED_PROJECT_NAME,
        "userId:$userId, projectName:$name",
    )
