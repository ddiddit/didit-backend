package com.didit.application.audit

import java.lang.annotation.Inherited

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class Audit(
    val action: AuditAction,
    val targetType: String = "",
)
