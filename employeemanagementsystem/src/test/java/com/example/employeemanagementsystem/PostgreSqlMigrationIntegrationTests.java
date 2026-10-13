package com.example.employeemanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "app.security.jwt.secret=postgres-ci-test-secret-at-least-32-characters",
        "app.security.bootstrap-admin.username=postgres-ci-admin@example.com",
        "app.security.bootstrap-admin.password=PostgresCiAdminPassword123!"
})
@ActiveProfiles("postgres-test")
class PostgreSqlMigrationIntegrationTests {

    @Autowired
    @Qualifier("mainDataSource")
    private DataSource postgresDataSource;

    @Test
    void flywayCreatesTheApplicationSchemaInPostgres() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(postgresDataSource);

        Integer applicationTableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('departments', 'employees', 'user_accounts')
                """, Integer.class);

        Integer successfulMigrationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1'
                  AND success = TRUE
                """, Integer.class);

        assertEquals(3, applicationTableCount);
        assertEquals(1, successfulMigrationCount);
    }
}