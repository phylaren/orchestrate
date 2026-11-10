package genius.project.orchestrate.audit.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    static final int ARGUMENTS_LENGTH = 4000;
    static final int RESULT_LENGTH = 4000;
    static final int ERROR_MESSAGE_LENGTH = 2000;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, updatable = false, length = 100)
    private String action;

    @Column(name = "method_name", nullable = false, updatable = false, length = 255)
    private String methodName;

    @Column(name = "arguments", updatable = false, length = ARGUMENTS_LENGTH)
    private String arguments;

    @Column(name = "result", updatable = false, length = RESULT_LENGTH)
    private String result;

    @Column(name = "success", nullable = false, updatable = false)
    private boolean success;

    @Column(name = "error_type", updatable = false, length = 255)
    private String errorType;

    @Column(name = "error_message", updatable = false, length = ERROR_MESSAGE_LENGTH)
    private String errorMessage;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private long durationMs;

    protected AuditLogEntity() {
    }

    AuditLogEntity(UUID id, Instant occurredAt, UUID userId, String action, String methodName,
                   String arguments, String result, boolean success, String errorType,
                   String errorMessage, long durationMs) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.userId = userId;
        this.action = action;
        this.methodName = methodName;
        this.arguments = arguments;
        this.result = result;
        this.success = success;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getArguments() {
        return arguments;
    }

    public String getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AuditLogEntity that = (AuditLogEntity) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
