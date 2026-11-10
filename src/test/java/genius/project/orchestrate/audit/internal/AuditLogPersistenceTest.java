package genius.project.orchestrate.audit.internal;

import genius.project.orchestrate.audit.AuditLogService;
import genius.project.orchestrate.audit.AuditRecord;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.common.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Без {@code @Transactional} на класі: тест сам керує бізнес-транзакцією, щоб перевірити,
 * що запис аудиту переживає її відкат.
 */
@SpringBootTest
class AuditLogPersistenceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-00000000aa01");
    private static final Instant AT = Instant.parse("2026-11-10T10:15:30Z");

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.sql("DELETE FROM audit_log").update();
        jdbc.sql("DELETE FROM users WHERE id = ?").param(USER_ID).update();
    }

    @Test
    @DisplayName("успішна операція: усі поля зберігаються в audit_log")
    void record_Success_ShouldPersistAllColumns() {
        auditLogService.record(AuditRecord.success(USER_ID, "CREATE_HOUSEHOLD",
                "HouseholdServiceImpl.create", "[name=Дім]", "HouseholdResponse[id=42]", AT, 17));

        List<AuditLogEntity> rows = repository.findByActionOrderByOccurredAtDesc("CREATE_HOUSEHOLD");
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getId()).isNotNull();
            assertThat(row.getUserId()).isEqualTo(USER_ID);
            assertThat(row.getMethodName()).isEqualTo("HouseholdServiceImpl.create");
            assertThat(row.getArguments()).isEqualTo("[name=Дім]");
            assertThat(row.getResult()).isEqualTo("HouseholdResponse[id=42]");
            assertThat(row.isSuccess()).isTrue();
            assertThat(row.getErrorType()).isNull();
            assertThat(row.getErrorMessage()).isNull();
            assertThat(row.getOccurredAt()).isEqualTo(AT.truncatedTo(ChronoUnit.MICROS));
            assertThat(row.getDurationMs()).isEqualTo(17);
        });
    }

    static Stream<Arguments> businessFailures() {
        return Stream.of(
                Arguments.of(new ValidationException("INVALID_NAME", "Назва не може бути порожньою")),
                Arguments.of(new BusinessRuleViolationException("OWNER_MUST_TRANSFER_OWNERSHIP",
                        "Спершу передайте власність")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("businessFailures")
    @DisplayName("помилка валідації / бізнес-правила: бізнес-зміни відкочено, запис аудиту залишився")
    void record_InsideRolledBackTransaction_ShouldSurviveRollback(RuntimeException businessError) {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbc.sql("INSERT INTO users (id, display_name, email, created_at) VALUES (?, 'Audit', 'audit@example.com', CURRENT_TIMESTAMP)")
                    .param(USER_ID).update();
            auditLogService.record(AuditRecord.failure(USER_ID, "LEAVE_HOUSEHOLD",
                    "HouseholdServiceImpl.leave", "[householdId=1]", businessError, AT, 5));
            throw businessError;
        })).isSameAs(businessError);

        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE id = ?").param(USER_ID)
                .query(Long.class).single())
                .as("бізнес-транзакцію відкочено")
                .isZero();
        assertThat(repository.findBySuccessFalseOrderByOccurredAtDesc()).singleElement().satisfies(row -> {
            assertThat(row.getAction()).isEqualTo("LEAVE_HOUSEHOLD");
            assertThat(row.isSuccess()).isFalse();
            assertThat(row.getErrorType()).isEqualTo(businessError.getClass().getName());
            assertThat(row.getErrorMessage()).isEqualTo(businessError.getMessage());
            assertThat(row.getResult()).isNull();
        });
    }

    @Test
    @DisplayName("збій запису аудиту не кидає винятку і не підміняє помилку бізнес-методу")
    void record_WhenDatabaseRejectsRow_ShouldNotThrow() {
        AuditRecord inconsistent = new AuditRecord(USER_ID, "BROKEN", "X.y", null, null,
                true, "java.lang.IllegalStateException", "успіх із помилкою порушує chk_audit_log_outcome", AT, 1);

        assertThatCode(() -> auditLogService.record(inconsistent)).doesNotThrowAnyException();
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("занадто довгі аргументи обрізаються до розміру колонки")
    void record_LongArguments_ShouldBeTruncated() {
        String huge = "x".repeat(AuditLogEntity.ARGUMENTS_LENGTH + 500);

        auditLogService.record(AuditRecord.success(null, "BULK", "X.bulk", huge, null, AT, 0));

        assertThat(repository.findByActionOrderByOccurredAtDesc("BULK")).singleElement().satisfies(row -> {
            assertThat(row.getUserId()).isNull();
            assertThat(row.getArguments()).hasSize(AuditLogEntity.ARGUMENTS_LENGTH).endsWith("…");
        });
    }
}
