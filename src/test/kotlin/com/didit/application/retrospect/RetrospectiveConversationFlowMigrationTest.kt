package com.didit.application.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.util.UUID

class RetrospectiveConversationFlowMigrationTest {
    @Test
    fun `V52 스크립트는 질문 허용 상태와 대화 액션을 추가하고 기존 완료 턴을 보정한다`() {
        val migration =
            requireNotNull(
                javaClass.getResource("/db/migration/V52__improve_retrospective_v2_conversation_flow.sql"),
            ).readText()
        DriverManager.getConnection("jdbc:h2:mem:conversation-flow-migration-${UUID.randomUUID()};MODE=MySQL", "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE retrospective_analysis_items (id BINARY(16) PRIMARY KEY)")
                statement.execute(
                    """
                    CREATE TABLE retrospective_conversation_turns (
                        id BINARY(16) PRIMARY KEY,
                        question_target VARCHAR(20),
                        status VARCHAR(20) NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    INSERT INTO retrospective_conversation_turns(id, question_target, status)
                    VALUES
                        (X'00000000000000000000000000000001', 'FACT', 'COMPLETED'),
                        (X'00000000000000000000000000000002', NULL, 'COMPLETED'),
                        (X'00000000000000000000000000000003', NULL, 'PROCESSING')
                    """.trimIndent(),
                )

                migration.split(";").filter { it.isNotBlank() }.forEach { statement.execute(it) }

                statement.execute(
                    "INSERT INTO retrospective_analysis_items(id) VALUES (X'00000000000000000000000000000004')",
                )
                statement.executeQuery("SELECT question_allowed FROM retrospective_analysis_items").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getBoolean("question_allowed")).isTrue()
                }
                statement.executeQuery("SELECT action FROM retrospective_conversation_turns ORDER BY id").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getString("action")).isEqualTo("ASK")
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getString("action")).isEqualTo("REFLECT")
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getString("action")).isNull()
                }
            }
        }
    }
}
