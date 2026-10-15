package com.didit.application.retrospect

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

@Testcontainers(disabledWithoutDocker = true)
class RetrospectiveResultV2MigrationTest {
    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")
    }

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `V55 스크립트는 스키마 2 결과를 내용 손실 없이 스키마 3으로 변환한다`() {
        val migration =
            requireNotNull(
                javaClass.getResource("/db/migration/V55__migrate_retrospective_v2_result_schema.sql"),
            ).readText()
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE retrospectives (
                        id INT PRIMARY KEY,
                        result_schema_version INT,
                        result_strength TEXT,
                        result_improvement TEXT,
                        result_process TEXT,
                        result_learning TEXT,
                        result_insight TEXT,
                        result_next_actions TEXT
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    INSERT INTO retrospectives VALUES
                        (1, 2, '빠르게 발견했다.', '확인이 부족했다.', '로그를 확인했다.', '체크리스트가 필요하다.',
                         '원인을 먼저 분리해야 한다.', '["테스트 추가", "체크리스트 작성", "알림 점검"]'),
                        (2, 2, NULL, '   ', NULL, NULL, NULL, '[]'),
                        (3, 3, '["기존 강점"]', NULL, NULL, NULL,
                         '{"title":"기존 제목","description":"기존 설명"}',
                         '[{"title":"기존 행동","description":"기존 행동 설명"}]'),
                        (4, 2, '보존할 강점', NULL, NULL, NULL, NULL,
                         '["행동 1", "행동 2", "행동 3", "행동 4"]'),
                        (5, 2, '손상된 값의 강점', NULL, NULL, NULL, NULL,
                         '["정상 문자열", 2]')
                    """.trimIndent(),
                )

                migration.split(";").filter { it.isNotBlank() }.forEach { statement.execute(it) }

                statement.executeQuery("SELECT * FROM retrospectives WHERE id = 1").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getInt("result_schema_version")).isEqualTo(3)
                    assertThat(objectMapper.readTree(rows.getString("result_strength")))
                        .isEqualTo(objectMapper.readTree("""["빠르게 발견했다."]"""))
                    assertThat(objectMapper.readTree(rows.getString("result_improvement")))
                        .isEqualTo(objectMapper.readTree("""["확인이 부족했다."]"""))
                    assertThat(objectMapper.readTree(rows.getString("result_process")))
                        .isEqualTo(objectMapper.readTree("""["로그를 확인했다."]"""))
                    assertThat(objectMapper.readTree(rows.getString("result_learning")))
                        .isEqualTo(objectMapper.readTree("""["체크리스트가 필요하다."]"""))
                    assertThat(objectMapper.readTree(rows.getString("result_insight")))
                        .isEqualTo(
                            objectMapper.readTree(
                                """{"title":"디딧의 인사이트","description":"원인을 먼저 분리해야 한다."}""",
                            ),
                        )
                    assertThat(objectMapper.readTree(rows.getString("result_next_actions")))
                        .isEqualTo(
                            objectMapper.readTree(
                                """[{"title":"다음 행동","description":"테스트 추가"},{"title":"다음 행동","description":"체크리스트 작성"},{"title":"다음 행동","description":"알림 점검"}]""",
                            ),
                        )
                }
                statement.executeQuery("SELECT * FROM retrospectives WHERE id = 2").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getInt("result_schema_version")).isEqualTo(3)
                    assertThat(rows.getString("result_strength")).isNull()
                    assertThat(rows.getString("result_improvement")).isNull()
                    assertThat(rows.getString("result_next_actions")).isNull()
                }
                statement.executeQuery("SELECT * FROM retrospectives WHERE id = 3").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getInt("result_schema_version")).isEqualTo(3)
                    assertThat(rows.getString("result_strength")).isEqualTo("[\"기존 강점\"]")
                    assertThat(rows.getString("result_insight"))
                        .isEqualTo("{\"title\":\"기존 제목\",\"description\":\"기존 설명\"}")
                    assertThat(rows.getString("result_next_actions"))
                        .isEqualTo("""[{"title":"기존 행동","description":"기존 행동 설명"}]""")
                }
                statement.executeQuery("SELECT * FROM retrospectives WHERE id = 4").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getInt("result_schema_version")).isEqualTo(2)
                    assertThat(rows.getString("result_strength")).isEqualTo("보존할 강점")
                    assertThat(rows.getString("result_next_actions"))
                        .isEqualTo("""["행동 1", "행동 2", "행동 3", "행동 4"]""")
                }
                statement.executeQuery("SELECT * FROM retrospectives WHERE id = 5").use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getInt("result_schema_version")).isEqualTo(2)
                    assertThat(rows.getString("result_strength")).isEqualTo("손상된 값의 강점")
                    assertThat(rows.getString("result_next_actions")).isEqualTo("""["정상 문자열", 2]""")
                }
            }
        }
    }
}
