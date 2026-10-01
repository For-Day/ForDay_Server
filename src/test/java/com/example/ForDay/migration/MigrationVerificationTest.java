package com.example.ForDay.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * db/migration의 마이그레이션이 실제 MySQL에 적용되는지, 그리고 적용 결과가 엔티티와
 * 맞는지 검증한다.
 *
 * <p>이 테스트가 통과한다는 것은 두 가지를 동시에 뜻한다.
 * <ol>
 *   <li>Flyway가 V1부터 마지막 버전까지 전부 성공적으로 적용했다 - 아래 단정으로 확인</li>
 *   <li>그 결과 스키마가 엔티티와 일치한다 - {@code ddl-auto: validate}라서, 어긋나면
 *       컨텍스트 기동 자체가 실패하고 이 테스트는 실행되지도 못한다</li>
 * </ol>
 *
 * <p>MySQL이 필요하므로 {@code migration} 태그로 분리했다. 기본 {@code test} 태스크에서는
 * 제외되고 {@code migrationTest} 태스크에서만 실행된다(build.gradle 참고).
 */
@SpringBootTest
@ActiveProfiles({"test", "migration"})
@Tag("migration")
class MigrationVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("모든 마이그레이션이 성공 상태로 적용된다")
    void allMigrationsSucceeded() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT version, description, type, success FROM flyway_schema_history ORDER BY installed_rank");

        assertThat(history).isNotEmpty();
        assertThat(history)
                .as("실패한 마이그레이션이 있으면 안 된다: %s", history)
                .allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
    }

    @Test
    @DisplayName("기준선 이후 마이그레이션이 비어 있지 않다 - V1만 있고 끝나는 상태를 막는다")
    void hasMigrationsBeyondBaseline() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'SQL'", Integer.class);

        assertThat(applied)
                .as("SQL 타입 마이그레이션이 한 건도 없으면 db/migration이 로드되지 않았다는 뜻이다")
                .isNotNull()
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("아웃박스 payload가 LONGTEXT다 - V2가 실제로 반영됐는지")
    void outboxPayloadIsLongtext() {
        String columnType = jdbcTemplate.queryForObject(
                "SELECT column_type FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'notification_outbox' "
                        + "AND column_name = 'payload'",
                String.class);

        assertThat(columnType).isEqualTo("longtext");
    }

    @Test
    @DisplayName("DB 기본 collation이 테이블들과 같다 - V3가 실제로 반영됐는지")
    void databaseDefaultCollationMatchesTables() {
        String dbCollation = jdbcTemplate.queryForObject(
                "SELECT default_collation_name FROM information_schema.schemata "
                        + "WHERE schema_name = DATABASE()",
                String.class);

        List<String> tableCollations = jdbcTemplate.queryForList(
                "SELECT DISTINCT table_collation FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_collation IS NOT NULL",
                String.class);

        assertThat(tableCollations)
                .as("테이블 collation이 섞여 있으면 안 된다")
                .hasSize(1);
        assertThat(dbCollation)
                .as("DB 기본값이 테이블들과 다르면, 새 테이블이 FK를 조용히 잃는다")
                .isEqualTo(tableCollations.get(0));
    }
}
