package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.Chore;
import genius.project.orchestrate.chore.internal.domain.ChoreWithParticipants;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntityMapper;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataChoreRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Primary
public class JpaChoreStore implements ChoreStore {

    private final SpringDataChoreRepository repository;

    public JpaChoreStore(SpringDataChoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Chore save(Chore chore) {
        ChoreEntity entity = repository.findById(chore.id())
                .map(existing -> {
                    ChoreEntityMapper.copyScalarsInto(chore, existing);
                    return existing;
                })
                .orElseGet(() -> ChoreEntityMapper.toEntity(chore));
        return ChoreEntityMapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chore> findById(UUID id) {
        return repository.findById(id).map(ChoreEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chore> findAll() {
        return repository.findAll().stream()
                .map(ChoreEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoreWithParticipants> findAllWithParticipants(UUID householdId) {
        List<ChoreEntity> entities = householdId == null
                ? repository.findAllWithParticipants()
                : repository.findAllByHouseholdIdWithParticipants(householdId);
        return entities.stream()
                .map(ChoreEntityMapper::toDomainWithParticipants)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChoreWithParticipants> findByIdWithParticipants(UUID id) {
        return repository.findByIdWithParticipants(id).map(ChoreEntityMapper::toDomainWithParticipants);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
