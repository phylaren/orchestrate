package genius.project.orchestrate.integration;

import genius.project.orchestrate.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class PostgresMigrationIntegrationTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    @DisplayName("тести працюють із справжнім PostgreSQL 17, а не з H2")
    void usesRealPostgres() {
        String version = jdbc.sql("select version()").query(String.class).single();

        assertThat(version).startsWith("PostgreSQL 17");
    }

    @Test
    @DisplayName("міграції Flyway V1–V3 успішно застосовані до PostgreSQL")
    void flywayMigrationsApplied() {
        Long applied = jdbc.sql("select count(*) from flyway_schema_history where success")
                .query(Long.class).single();

        assertThat(applied).isGreaterThanOrEqualTo(3);
    }
}