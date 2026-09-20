package com.didit.application.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

class RetrospectiveFeedbackMigrationTest {
    @Test
    fun `V51 스크립트는 독립 평가와 사유 테이블 및 중복 제약을 생성한다`() {
        val migration = requireNotNull(javaClass.getResource("/db/migration/V51__create_retrospective_feedbacks.sql")).readText()
        DriverManager.getConnection("jdbc:h2:mem:feedback-migration-${UUID.randomUUID()};MODE=MySQL", "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                migration.split(";").filter { it.isNotBlank() }.forEach { statement.execute(it) }
                statement.execute(
                    "INSERT INTO retrospective_feedbacks(id, retrospective_id, rating) VALUES (X'00000000000000000000000000000001', X'00000000000000000000000000000002', 'HELPFUL')",
                )
                statement.execute(
                    "INSERT INTO retrospective_feedback_reasons(feedback_id, reason_order, reason) VALUES (X'00000000000000000000000000000001', 0, 'CORE_IDENTIFIED')",
                )
                assertThrows<SQLException> {
                    statement.execute(
                        "INSERT INTO retrospective_feedbacks(id, retrospective_id, rating) VALUES (X'00000000000000000000000000000003', X'00000000000000000000000000000002', 'HELPFUL')",
                    )
                }
                statement.executeQuery("SELECT COUNT(*) FROM retrospective_feedback_reasons").use { rows ->
                    rows.next()
                    assertThat(rows.getInt(1)).isEqualTo(1)
                }
            }
        }
    }
}
