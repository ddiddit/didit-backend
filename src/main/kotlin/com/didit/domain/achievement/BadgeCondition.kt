package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
class BadgeCondition(
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 50)
    val conditionType: BadgeConditionType,
    @Column(name = "threshold", nullable = false)
    val threshold: Int,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "JSON")
    val params: Map<String, Any>? = null,
) {
    fun weeklyMinCount(): Int = (params?.get("weeklyMinCount") as? Number)?.toInt() ?: 1
}
