package genius.project.orchestrate.audit.internal;

import genius.project.orchestrate.audit.AuditLogService;
import genius.project.orchestrate.audit.AuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class JpaAuditLogService implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(JpaAuditLogService.class);
    private static final String TRUNCATION_MARK = "…";

    private final AuditLogWriter writer;

    JpaAuditLogService(AuditLogWriter writer) {
        this.writer = writer;
    }

    @Override
    public void record(AuditRecord record) {
        try {
            writer.save(toEntity(record));
        } catch (RuntimeException ex) {
            // Збій аудиту не повинен підміняти виняток бізнес-методу, який аспект прокидає далі.
            log.error("Не вдалося зберегти запис аудиту: action={}, method={}",
                    record.action(), record.methodName(), ex);
        }
    }

    private static AuditLogEntity toEntity(AuditRecord record) {
        return new AuditLogEntity(
                UUID.randomUUID(),
                record.occurredAt(),
                record.userId(),
                record.action(),
                record.methodName(),
                truncate(record.arguments(), AuditLogEntity.ARGUMENTS_LENGTH),
                truncate(record.result(), AuditLogEntity.RESULT_LENGTH),
                record.success(),
                record.errorType(),
                truncate(record.errorMessage(), AuditLogEntity.ERROR_MESSAGE_LENGTH),
                Math.max(0, record.durationMs()));
    }

    static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - TRUNCATION_MARK.length()) + TRUNCATION_MARK;
    }
}
