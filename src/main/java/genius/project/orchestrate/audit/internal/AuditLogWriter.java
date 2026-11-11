package genius.project.orchestrate.audit.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class AuditLogWriter {

    private final AuditLogRepository repository;

    AuditLogWriter(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLogEntity entry) {
        repository.save(entry);
    }
}