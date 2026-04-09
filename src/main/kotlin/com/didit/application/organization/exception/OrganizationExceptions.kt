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

class ProjectNotFoundException(
    projectId: UUID,
) : BusinessException(
        OrganizationErrorCode.PROJECT_NOT_FOUND,
        "projectId:$projectId not found",
    )

class DuplicateTagNameException(
    userId: UUID,
    tagName: String,
) : BusinessException(
        OrganizationErrorCode.DUPLICATED_TAG_NAME,
        "userId: $userId, tagName: $tagName",
    )

class TagNotFoundException(
    tagId: UUID,
) : BusinessException(
        OrganizationErrorCode.TAG_NOT_FOUND,
        "tagId: $tagId not found",
    )
