package com.didit.adapter.persistence.admin

import com.didit.application.admin.required.AdminAnalyticsRepository
import com.didit.application.admin.required.AdminAnalyticsSnapshot
import com.didit.application.admin.required.AnalyticsRetrospective
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Base64

@Repository
class AdminAnalyticsAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : AdminAnalyticsRepository {
    override fun load(
        from: LocalDateTime,
        to: LocalDateTime,
        launchAt: LocalDateTime,
        now: LocalDateTime,
    ): AdminAnalyticsSnapshot {
        val retrospectives =
            jdbcTemplate.query(
                """
                SELECT r.status,
                       CASE WHEN r.completed_at IS NULL THEN NULL
                            ELSE TIMESTAMPDIFF(SECOND, r.created_at, r.completed_at) END AS elapsed_seconds,
                       (SELECT COUNT(*) FROM chat_messages m
                        WHERE m.retrospective_id = r.id AND m.sender = 'USER' AND m.is_skipped = FALSE) AS answer_count
                FROM retrospectives r
                JOIN users u ON u.id = r.user_id
                WHERE u.account_type = 'CUSTOMER' AND r.created_at >= ? AND r.created_at < ?
                """.trimIndent(),
                { rs, _ ->
                    AnalyticsRetrospective(
                        completed = rs.getString("status") == "COMPLETED",
                        answerCount = rs.getInt("answer_count"),
                        elapsedDurationSeconds = rs.getObject("elapsed_seconds")?.let { rs.getLong("elapsed_seconds") },
                    )
                },
                from,
                to,
            )

        val completions =
            jdbcTemplate
                .query(
                    """
                    SELECT r.user_id, r.completed_at
                    FROM retrospectives r
                    JOIN users u ON u.id = r.user_id
                    WHERE u.account_type = 'CUSTOMER' AND r.status = 'COMPLETED' AND r.completed_at IS NOT NULL
                    ORDER BY r.completed_at
                    """.trimIndent(),
                    { rs, _ ->
                        Base64.getEncoder().encodeToString(rs.getBytes("user_id")) to
                            rs.getTimestamp("completed_at").toLocalDateTime()
                    },
                ).groupBy({ it.first }, { it.second })

        val observationCutoff = now.minusDays(14)
        val firstCompletions = completions.values.mapNotNull { it.firstOrNull() }.filter { it >= from && it < to }
        val eligibleUsers =
            completions.filterValues { dates ->
                val first = dates.firstOrNull() ?: return@filterValues false
                first >= from && first < to && first <= observationCutoff
            }
        val returnedCount =
            eligibleUsers.values
                .count { dates ->
                    val first = dates.first()
                    dates.drop(1).any { !it.isAfter(first.plusDays(14)) }
                }.toLong()

        val returnFrom = maxOf(from, launchAt)
        val returningUserCount =
            if (returnFrom >= to) {
                0
            } else {
                count(
                    """
                    SELECT COUNT(DISTINCT u.id) FROM users u
                    JOIN retrospectives r ON r.user_id = u.id
                    WHERE u.account_type = 'CUSTOMER' AND u.created_at < ?
                      AND r.created_at >= ? AND r.created_at < ?
                    """.trimIndent(),
                    launchAt,
                    returnFrom,
                    to,
                )
            }

        return AdminAnalyticsSnapshot(
            retrospectives = retrospectives,
            newUserCount =
                count(
                    "SELECT COUNT(*) FROM users WHERE account_type = 'CUSTOMER' AND created_at >= ? AND created_at < ?",
                    from,
                    to,
                ),
            returningUserCount = returningUserCount,
            retentionEligibleCount = eligibleUsers.size.toLong(),
            retentionReturnedCount = returnedCount,
            retentionObservingCount = (firstCompletions.size - eligibleUsers.size).toLong(),
        )
    }

    private fun count(
        sql: String,
        vararg args: Any,
    ): Long = jdbcTemplate.queryForObject(sql, Long::class.java, *args) ?: 0
}
