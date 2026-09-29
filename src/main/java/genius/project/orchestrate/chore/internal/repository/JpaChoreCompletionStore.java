package genius.project.orchestrate.chore.internal.repository;

import genius.project.orchestrate.chore.internal.domain.ChoreCompletion;
import genius.project.orchestrate.chore.internal.persistence.ChoreCompletionEntity;
import genius.project.orchestrate.chore.internal.persistence.ChoreEntityMapper;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataChoreCompletionRepository;
import genius.project.orchestrate.chore.internal.repository.jpa.SpringDataChoreRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Primary
public class JpaChoreCompletionStore implements ChoreCompletionStore {

    private final SpringDataChoreCompletionRepository completionRepository;
    private final SpringDataChoreRepository choreRepository;

    public JpaChoreCompletionStore(SpringDataChoreCompletionRepository completionRepository,
                                   SpringDataChoreRepository choreRepository) {
        this.completionRepository = completionRepository;
        this.choreRepository = choreRepository;
    }

    @Override
    public ChoreCompletion save(ChoreCompletion completion) {
        var choreProxy = choreRepository.getReferenceById(completion.choreId());
        var entity = new ChoreCompletionEntity(
                completion.id(),
                choreProxy,
                completion.completedByUserId(),
                completion.completedAt(),
                completion.status(),
                completion.confirmedByUserId(),
                completion.confirmedAt()
        );
        var saved = completionRepository.save(entity);
        return ChoreEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<ChoreCompletion> findById(UUID completionId) {
        return completionRepository.findById(completionId).map(ChoreEntityMapper::toDomain);
    }

    @Override
    public List<ChoreCompletion> findByChoreId(UUID choreId) {
        return completionRepository.findByChoreId(choreId).stream()
                .map(ChoreEntityMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID completionId) {
        completionRepository.deleteById(completionId);
    }
}
