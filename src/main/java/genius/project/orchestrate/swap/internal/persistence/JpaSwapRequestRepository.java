package genius.project.orchestrate.swap.internal.persistence;

import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.exception.SwapRequestNotFoundException;
import genius.project.orchestrate.swap.internal.SwapRequestRepository;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
class JpaSwapRequestRepository implements SwapRequestRepository {

    private final SwapRequestJpaRepository repository;
    private final SwapRequestPersistenceMapper mapper;

    JpaSwapRequestRepository(SwapRequestJpaRepository repository, SwapRequestPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SwapRequest save(SwapRequest swapRequest) {
        return mapper.toDomain(repository.save(mapper.toEntity(swapRequest)));
    }

    @Override
    public Optional<SwapRequest> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SwapRequest> findByChoreId(UUID choreId) {
        return repository.findByChoreIdOrderByCreatedAtDesc(choreId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<SwapRequest> findByChoreIdAndStatus(UUID choreId, SwapRequestStatus status) {
        return repository.findByChoreIdAndStatusOrdered(choreId, status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsPending(UUID choreId, UUID initiatorUserId, UUID receiverUserId) {
        return repository.existsByChoreIdAndInitiatorUserIdAndReceiverUserIdAndStatus(
                choreId, initiatorUserId, receiverUserId, SwapRequestStatus.PENDING);
    }

    @Override
    public SwapRequest update(SwapRequest swapRequest) {
        SwapRequestEntity entity = repository.findById(swapRequest.id())
                .orElseThrow(() -> new SwapRequestNotFoundException(swapRequest.id()));
        mapper.applyMutableTerms(swapRequest, entity);
        return mapper.toDomain(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public long countByStatus(UUID choreId, SwapRequestStatus status) {
        return repository.countByChoreIdAndStatus(choreId, status);
    }

    @Override
    public Map<SwapRequestStatus, Long> countGroupedByStatus(UUID choreId) {
        Map<SwapRequestStatus, Long> counts = new EnumMap<>(SwapRequestStatus.class);
        for (SwapRequestStatus status : SwapRequestStatus.values()) {
            counts.put(status, 0L);
        }
        repository.countByStatusNative(choreId)
                .forEach(row -> counts.put(SwapRequestStatus.valueOf(row.getStatus()), row.getTotal()));
        return counts;
    }
}
