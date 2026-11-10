package genius.project.orchestrate.audit.internal;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends ListCrudRepository<AuditLogEntity, UUID> {

    /**
     * Окрема транзакція: відкат бізнес-транзакції, у межах якої стався виклик, не знищує запис аудиту.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    <S extends AuditLogEntity> S save(S entity);

    List<AuditLogEntity> findByActionOrderByOccurredAtDesc(String action);

    List<AuditLogEntity> findByUserIdOrderByOccurredAtDesc(UUID userId);

    List<AuditLogEntity> findBySuccessFalseOrderByOccurredAtDesc();
}
